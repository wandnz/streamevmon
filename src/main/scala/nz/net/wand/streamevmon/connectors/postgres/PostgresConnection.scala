/* This file is part of streamevmon.
 *
 * Copyright (C) 2021  The University of Waikato, Hamilton, New Zealand
 *
 * Author: Daniel Oosterwijk
 *
 * All rights reserved.
 *
 * This code has been developed by the University of Waikato WAND
 * research group. For further information please see https://wand.nz,
 * or our Github organisation at https://github.com/wanduow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.net.wand.streamevmon.connectors.postgres

import nz.net.wand.streamevmon.{Caching, Logging}
import nz.net.wand.streamevmon.connectors.postgres.PostgresSchema._
import nz.net.wand.streamevmon.connectors.postgres.SquerylEntrypoint._
import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.traits.{Measurement, PostgresMeasurementMeta}

import java.sql.DriverManager
import java.time.Instant

import org.apache.flink.api.java.utils.ParameterTool
import org.squeryl.Session
import org.squeryl.adapters.PostgreSqlAdapter

import scala.concurrent.duration._
import scala.util.Try

/** Contains additional apply methods for the companion class.
  */
object PostgresConnection extends Caching {

  /** Creates a new PostgresConnection from the given config. Expects all fields
    * specified in the package object documentation to be present.
    *
    * @param p            The configuration to use. Generally obtained from the Flink
    *                     global configuration.
    * @param configPrefix A custom config prefix to use, in case the configuration
    *                     object is not as expected.
    *
    * @return A new PostgresConnection object.
    */
  def apply(p: ParameterTool, configPrefix: String = "postgres"): PostgresConnection = {
    PostgresConnection(
      p.get(s"source.$configPrefix.serverName"),
      p.getInt(s"source.$configPrefix.portNumber"),
      p.get(s"source.$configPrefix.databaseName"),
      p.get(s"source.$configPrefix.user"),
      p.get(s"source.$configPrefix.password"),
      p.getInt("caching.ttl")
    ).withMemcachedIfEnabled(p)
  }

  /** Creates a new PostgresConnection object from a JDBC URL along with the
    * username, password, and desired caching TTL.
    *
    * @return A new PostgresConnection object.
    */
  def apply(
    jdbcUrl: String,
    user: String,
    password: String,
    caching_ttl: Int
  ): PostgresConnection = {

    val parts = jdbcUrl
      .stripPrefix("jdbc:postgresql://")
      .split(Array(':', '/', '?'))

    PostgresConnection(
      parts(0),
      parts(1).toInt,
      parts(2),
      user,
      password,
      caching_ttl
    )
  }
}

/** PostgreSQL interface which produces
  * [[nz.net.wand.streamevmon.measurements.traits.PostgresMeasurementMeta MeasurementMeta]]
  * objects. See the package description for configuration details for normal
  * usage.
  */
case class PostgresConnection(
  host: String,
  port  : Int,
  databaseName: String,
  user        : String,
  password    : String,
  caching_ttl : Int
) extends Caching with Logging {

  @transient protected lazy val ttl: Option[FiniteDuration] = {
    if (caching_ttl == 0) {
      None
    }
    else {
      Some(caching_ttl.seconds)
    }
  }

  protected def jdbcUrl: String = {
    s"jdbc:postgresql://$host:$port/$databaseName?loggerLevel=OFF"
  }

  @transient protected var session: Option[Session] = None

  /** Enables Memcached caching if required by the specified configuration.
    *
    * @see [[nz.net.wand.streamevmon.Caching Caching]] for relevant config keys.
    */
  def withMemcachedIfEnabled(p: ParameterTool): PostgresConnection = {
    if (p.getBoolean("caching.memcached.enabled")) {
      useMemcached(p)
    }
    this
  }

  def closeSession(): Unit = {
    session.foreach { sess => sess.close }
  }

  protected def getOrInitSession(): Option[Session] = {
    session match {
      case Some(_) => session
      case None =>
        session = Try {
          Class.forName("org.postgresql.Driver")
          Session.create(
            DriverManager.getConnection(jdbcUrl, user, password),
            new PostgreSqlAdapter
          )
        }.fold(
          e => {
            logger.error(s"Could not initialise PostgreSQL session for $jdbcUrl with user $user! $e")
            None
          },
          sess => Some(sess)
        )
        session
    }
  }

  /** AMP measurements require integer stream IDs. Currently this class only
    * supports AMP measurements, so we just check this for all inputs.
    */
  private def validateStreamIsInteger(meas: Measurement): Unit = {
    try {
      meas.stream.toInt
    }
    catch {
      case _: NumberFormatException => throw new NumberFormatException(
        s"Measurement of type ${meas.getClass.getCanonicalName} had non-integer " +
          s"stream ID ${meas.stream} of type ${meas.stream.getClass.getCanonicalName}!"
      )
      case e: Throwable => throw e
    }
  }

  /** Abstracts the setup and error handling code for a Squeryl operation. The
    * `func` that gets passed will be wrapped in an `inTransaction` block, so
    * you can use any Squeryl constructions you like. Returns None if the
    * transaction failed - error messages go to the logger.
    */
  protected def doGet[T](func: => T): Option[T] = {
    getOrInitSession() match {
      case None => None
      case Some(sess) =>
        using(sess) {
          Try(
            inTransaction(func)
          ).fold(
            e => {
              logger.error(s"Failed Postgres transaction! $e")
              None
            },
            tr => Some(tr)
          )
        }
    }
  }

  /** Abstracts the getWithCache(doGet()) pattern. Supply a cache key and the
    * function to pass to doGet. Returns None if any layer failed - error
    * messages go to the logger.
    */
  protected def doGetWithCache[T](cacheKey: String, func: => T): Option[T] = {
    val result = getWithCache[Option[T]](cacheKey, ttl, doGet(func))
    if (result.isEmpty) {
      invalidate(cacheKey)
      None
    }
    result
  }

  /** Gets the metadata associated with a given measurement. Uses caching.
    *
    * @param base The measurement to gather metadata for.
    *
    * @return The metadata if successful, otherwise None.
    */
  def getMeta(base: Measurement): Option[PostgresMeasurementMeta] = {
    doGetWithCache(
      s"${base.getClass.getSimpleName}.${base.stream}",
      {
        validateStreamIsInteger(base)
        base match {
          // It looks like we can't perform m.stream.toString, since Squeryl starts complaining about not knowing
          // how to cast integers to strings in a PostgreSQL context. Instead, we'll cast our stream ID to an int,
          // which of course might fail. If it does fail, we'll return a known false expression instead.
          case _: ICMP => icmpMeta.where(m => Try(m.stream === base.stream.toInt).getOrElse(true === false)).headOption
          case _: DNS => dnsMeta.where(m => Try(m.stream === base.stream.toInt).getOrElse(true === false)).headOption
          case _: Traceroute | _: TraceroutePathlen =>
            tracerouteMeta.where(m => Try(m.stream === base.stream.toInt).getOrElse(true === false)).headOption
          case _: TCPPing =>
            tcppingMeta.where(m => Try(m.stream === base.stream.toInt).getOrElse(true === false)).headOption
          case _: HTTP => httpMeta.where(m => Try(m.stream === base.stream.toInt).getOrElse(true === false)).headOption
          case _ => None
        }
      }
    ).flatten
  }

  /** Gets all the TracerouteMeta entries in the database. This can be used
    * to discover all the Traceroute streams stored, so you can then query them.
    *
    * Unlike `getMeta`, this method does not use caching, since we mainly use it
    * to rediscover new streams rather than picking out data about a particular
    * stream.
    *
    * @return A collection of TracerouteMeta, or None if the query failed.
    */
  def getAllTracerouteMeta: Option[Iterable[TracerouteMeta]] = {
    doGet(from(tracerouteMeta)(m => select(m)).toList)
  }

  /** Gets all TraceroutePaths for a particular stream. This function does not
    * use caching, since whenever we call it we want to be sure we have the most
    * up-to-date information.
    */
  def getAllTraceroutePaths(stream: Int): Option[Iterable[TraceroutePath]] = {
    doGet(from(traceroutePath(stream))(m => select(m)).toList)
  }

  /** Gets all TracerouteAsPaths for a particular stream. This function does not
    * use caching, since whenever we call it we want to be sure we have the most
    * up-to-date information.
    */
  def getAllTracerouteAsPaths(stream: Int): Option[Iterable[TracerouteAsPath]] = {
    doGet(from(tracerouteAsPath(stream))(m => select(m)).toList)
  }

  /** Gets some AMP Traceroute measurements from PostgreSQL. Most other AMP
    * measurements are in InfluxDB, so this one is a little unusual. We don't
    * use caching here, since each call is likely to be unique, and it would be
    * a waste of time and effort.
    *
    * @param stream The stream ID of the desired measurements. Currently only
    *               supports querying a single stream at a time.
    * @param start  The oldest measurement should be no older than this.
    * @param end    The newest measurement should be no newer than this.
    *
    * @return A collection of Traceroute measurements, or None if an error
    *         occurred. The error is logged.
    */
  def getTracerouteData(
    stream: Int,
    start : Instant = Instant.EPOCH,
    end   : Instant = Instant.now()
  ): Option[Iterable[Traceroute]] = {
    doGet {
      from(traceroute(stream))(t =>
        where(
          // between is inclusive on both ends. We want it to be
          // exclusive on the left, and inclusive on the right.
          t.timestamp between(start.getEpochSecond + 1, end.getEpochSecond)
        ).select(t)
      ).toList
    }
  }

  /** Gets a traceroute path from Postgres. The TracerouteMeta associated with
    * the provided stream ID will contain the relevant metadata about the source
    * and destination of this path.
    *
    * We use caching here since there are likely to be many duplicate calls to
    * get the same path as we encounter it again and again.
    */
  def getTraceroutePath(stream: Int, pathId: Int): Option[TraceroutePath] = {
    doGetWithCache(
      s"traceroute_path.$stream.$pathId",
      traceroutePath(stream).where(_.path_id === pathId).head
    )
  }

  def getTraceroutePath(trace: Traceroute): Option[TraceroutePath] = {
    validateStreamIsInteger(trace)
    getTraceroutePath(trace.stream.toInt, trace.path_id)
  }

  /** Gets a traceroute AS-path from Postgres, detailing the autonomous systems
    * that each of the hops along the way were in. The TracerouteMeta associated
    * with the provided stream ID will contain the relevant metadata about the
    * source and destination of this path.
    *
    * We use caching here since there are likely to be many duplicate calls to
    * get the same path as we encounter it again and again.
    */
  def getTracerouteAsPath(stream: Int, asPathId: Int): Option[TracerouteAsPath] = {
    doGetWithCache(
      s"traceroute_aspath.$stream.$asPathId",
      tracerouteAsPath(stream).where(_.aspath_id === asPathId).head
    )
  }

  def getTracerouteAsPath(trace: Traceroute): Option[TracerouteAsPath] = {
    validateStreamIsInteger(trace)
    trace.aspath_id.flatMap(asPathId => getTracerouteAsPath(trace.stream.toInt, asPathId))
  }
}
