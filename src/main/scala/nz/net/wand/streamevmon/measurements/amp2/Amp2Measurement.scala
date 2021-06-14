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

package nz.net.wand.streamevmon.measurements.amp2

import nz.net.wand.streamevmon.connectors.influx.LineProtocol
import nz.net.wand.streamevmon.measurements.traits.{HasDefault, Measurement}

import scala.reflect.runtime.universe._

/** The parent class for all measurements collected by amplet2. Provides some
  * useful defaults for subclasses, including implementing `stream` based on
  * the tags declared by the subclass.
  */
trait Amp2Measurement extends Measurement with HasDefault {
  // These three tags are present for all measurements.
  val source: String
  val destination: String
  val test: String

  /** The name of the measurement as it appears in InfluxDB.
    */
  val measurementName: String

  /** The unique tags for the measurement type. Do not include `source`,
    * `destination`, `test`, or `measurementName`. When combined with those four
    * fields, the contents of this field should create a unique identifier for
    * a measurement stream.
    */
  val tags: Seq[Any]

  override lazy val stream: String = (
    Seq(measurementName, source, destination, test) ++
      tags.map(_.toString)
    ).mkString(Amp2Measurement.streamTagSeparator)

  override def isLossy: Boolean = defaultValue.isEmpty
}

/** Allows creating any type of Amp2Measurement based on the data provided.
  */
object Amp2Measurement {
  val streamTagSeparator = "--"

  /** Returns a collection containing the database column names associated with
    * a type, in the same order as the case class declares them.
    */
  def getColumnNames[T: TypeTag]: Seq[String] = {
    if (typeOf[T] == typeOf[Latency]) {
      getColumnNamesLatency
    }
    else {
      "time" +: typeOf[T].members.sorted.collect {
        case m: MethodSymbol if m.isCaseAccessor => m.name.toString
      }.filterNot(_ == "time")
    }
  }

  /** A special case is required here, since Latency is an abstract class with
    * several similar implementations.
    */
  protected def getColumnNamesLatency: Seq[String] = {
    val dns = getColumnNames[LatencyDns].filterNot(_ == "time")
    val icmp = getColumnNames[LatencyIcmp].filterNot(_ == "time")
    val tcpping = getColumnNames[LatencyTcpping].filterNot(_ == "time")
    "time" +: dns.union(icmp).union(tcpping)
  }

  /** Given an InfluxDB line protocol entry, returns the corresponding
    * Amp2Measurement, or None if any errors occurred or a matching measurement
    * type is not supported.
    */
  def createFromLineProtocol(line: String): Option[Amp2Measurement] = {
    val proto = LineProtocol(line)
    proto.flatMap { p =>
      p.measurementName match {
        case External.measurementName => External.create(p)
        case Fastping.measurementName => Fastping.create(p)
        case Http.measurementName => Http.create(p)
        case Latency.measurementName => Latency.create(p)
        case Pathlen.measurementName => Pathlen.create(p)
        case Sip.measurementName => Sip.create(p)
        case Throughput.measurementName => Throughput.create(p)
        case Traceroute.measurementName => Traceroute.create(p)
        case Udpstream.measurementName => Udpstream.create(p)
        case Video.measurementName => Video.create(p)
        case _ => None
      }
    }
  }
}
