package nz.net.wand.streamevmon.connectors.postgres

import nz.net.wand.streamevmon.{Caching, Logging}
import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.measurements.amp._

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
    * specified in the companion class' main documentation to be present.
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
  * [[nz.net.wand.streamevmon.measurements.amp.PostgresMeasurementMeta MeasurementMeta]]
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

  protected var session: Option[Session] = None

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
            logger.error(s"Could not initialise PostgreSQL session! $e")
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

  // There's a fair bit of duplicated code in these getX functions, but it's
  // not really worth the effort to extract it into a function at this point.

  /** Gets the metadata associated with a given measurement. Uses caching.
    *
    * @param base The measurement to gather metadata for.
    *
    * @return The metadata if successful, otherwise None.
    */
  def getMeta(base: Measurement): Option[PostgresMeasurementMeta] = {
    val cacheKey = s"${base.getClass.getSimpleName}.${base.stream}"
    val result: Option[PostgresMeasurementMeta] = getWithCache(
      cacheKey,
      ttl, {
        getOrInitSession() match {
          case None => None
          case Some(sess) =>
            import nz.net.wand.streamevmon.connectors.postgres.PostgresSchema._
            import nz.net.wand.streamevmon.connectors.postgres.SquerylEntrypoint._
            using(sess) {
              validateStreamIsInteger(base)
              transactionCounter += 1
              base match {
                // It looks like we can't perform m.stream.toString, since Squeryl starts complaining about not knowing
                // how to cast integers to strings in a PostgreSQL context. Instead, we'll cast our stream ID to an int,
                // which of course might fail. If it does fail, we'll return a known false expression instead.
                case _: ICMP => inTransaction(icmpMeta.where(m => Try(m.stream === base.stream.toInt).getOrElse(true === false)).headOption)
                case _: DNS => inTransaction(dnsMeta.where(m => Try(m.stream === base.stream.toInt).getOrElse(true === false)).headOption)
                case _: Traceroute | _: TraceroutePathlen =>
                  inTransaction(tracerouteMeta.where(m => Try(m.stream === base.stream.toInt).getOrElse(true === false)).headOption)
                case _: TCPPing =>
                  inTransaction(tcppingMeta.where(m => Try(m.stream === base.stream.toInt).getOrElse(true === false)).headOption)
                case _: HTTP => inTransaction(httpMeta.where(m => Try(m.stream === base.stream.toInt).getOrElse(true === false)).headOption)
                case _ =>
                  // easier to increment then decrement than to increment in all the valid paths
                  transactionCounter -= 1
                  None
              }
            }
        }
      }
    )
    if (result.isEmpty) {
      invalidate(cacheKey)
    }
    result
  }

  /** Gets all the TracerouteMeta entries in the database. This can be used
    * to discover all the Traceroute streams stored, so you can then query them.
    *
    * Unlike `getMeta`, this method does not use caching.
    *
    * @return A collection of TracerouteMeta, or None if the query failed.
    */
  def getAllTracerouteMeta: Option[Iterable[TracerouteMeta]] = {
    getOrInitSession() match {
      case None => None
      case Some(value) =>
        import nz.net.wand.streamevmon.connectors.postgres.PostgresSchema._
        import nz.net.wand.streamevmon.connectors.postgres.SquerylEntrypoint._
        using(value) {
          Try(
            inTransaction(from(tracerouteMeta)(m => select(m)).toList)
          ).fold(
            e => {
              logger.error(s"Failed Postgres transaction! $e")
              None
            },
            tr => {
              transactionCounter += 1
              Some(tr)
            }
          )
        }
    }
  }

  def getAllTraceroutePaths(stream: Int): Option[Iterable[TraceroutePath]] = {
    getOrInitSession() match {
      case None => None
      case Some(value) =>
        import nz.net.wand.streamevmon.connectors.postgres.PostgresSchema._
        import nz.net.wand.streamevmon.connectors.postgres.SquerylEntrypoint._
        using(value) {
          Try(
            inTransaction(from(traceroutePath(stream))(m => select(m)).toList)
          ).fold(
            e => {
              logger.error(s"Failed Postgres transaction! $e")
              None
            },
            tr => {
              transactionCounter += 1
              Some(tr)
            }
          )
        }
    }
  }

  def getAllTracerouteAsPaths(stream: Int): Option[Iterable[TracerouteAsPath]] = {
    getOrInitSession() match {
      case None => None
      case Some(value) =>
        import nz.net.wand.streamevmon.connectors.postgres.PostgresSchema._
        import nz.net.wand.streamevmon.connectors.postgres.SquerylEntrypoint._
        using(value) {
          Try(
            inTransaction(from(tracerouteAsPath(stream))(m => select(m)).toList)
          ).fold(
            e => {
              logger.error(s"Failed Postgres transaction! $e")
              None
            },
            tr => {
              transactionCounter += 1
              Some(tr)
            }
          )
        }
    }
  }

  /** Gets some AMP Traceroute measurements from PostgreSQL. Most other AMP
    * measurements are in InfluxDB, so this one is a little unusual.
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
    getOrInitSession() match {
      case None => None
      case Some(value) =>
        import nz.net.wand.streamevmon.connectors.postgres.PostgresSchema._
        import nz.net.wand.streamevmon.connectors.postgres.SquerylEntrypoint._
        using(value) {
          Try(
            inTransaction(
              from(traceroute(stream))(t =>
                where(
                  t.timestamp between(start.getEpochSecond, end.getEpochSecond)
                ).select(t)
              ).toList
            )
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

  /** Gets a traceroute path from Postgres. The TracerouteMeta associated with
    * the provided stream ID will contain the relevant metadata about the source
    * and destination of this path.
    */
  def getTraceroutePath(stream: Int, pathId: Int): Option[TraceroutePath] = {
    val cacheKey = s"traceroute_path.$stream.$pathId"
    val result: Option[TraceroutePath] = getWithCache(
      cacheKey,
      ttl, {
        getOrInitSession() match {
          case None => None
          case Some(value) =>
            import nz.net.wand.streamevmon.connectors.postgres.PostgresSchema._
            import nz.net.wand.streamevmon.connectors.postgres.SquerylEntrypoint._
            using(value) {
              Try(
                inTransaction(traceroutePath(stream).where(_.path_id === pathId).headOption)
              ).fold(
                e => {
                  logger.error(s"Failed Postgres transaction! $e")
                  None
                },
                tr => {
                  transactionCounter += 1
                  Some(tr)
                }
              )
            }
        }
      }
    )
    if (result.isEmpty) {
      invalidate(cacheKey)
    }
    result
  }

  def getTraceroutePath(trace: Traceroute): Option[TraceroutePath] = {
    validateStreamIsInteger(trace)
    getTraceroutePath(trace.stream.toInt, trace.path_id)
  }

  /** Gets a traceroute AS-path from Postgres, detailing the autonomous systems
    * that each of the hops along the way were in. The TracerouteMeta associated
    * with the provided stream ID will contain the relevant metadata about the
    * source and destination of this path.
    */
  def getTracerouteAsPath(stream: Int, asPathId: Int): Option[TracerouteAsPath] = {
    val cacheKey = s"traceroute_aspath.$stream.$asPathId"
    val result: Option[TracerouteAsPath] = getWithCache(
      cacheKey,
      ttl, {
        getOrInitSession() match {
          case None => None
          case Some(value) =>
            import nz.net.wand.streamevmon.connectors.postgres.PostgresSchema._
            import nz.net.wand.streamevmon.connectors.postgres.SquerylEntrypoint._
            using(value) {
              Try(
                inTransaction(tracerouteAsPath(stream).where(_.aspath_id === asPathId).headOption)
              ).fold(
                e => {
                  logger.error(s"Failed Postgres transaction! $e")
                  None
                },
                tr => {
                  transactionCounter += 1
                  Some(tr)
                }
              )
            }
        }
      }
    )
    if (result.isEmpty) {
      invalidate(cacheKey)
    }
    result
  }

  def getTracerouteAsPath(trace: Traceroute): Option[TracerouteAsPath] = {
    validateStreamIsInteger(trace)
    trace.aspath_id.flatMap(asPathId => getTracerouteAsPath(trace.stream.toInt, asPathId))
  }

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

  var transactionCounter = 0
}
