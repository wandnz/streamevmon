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

package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.measurements.traits._

import java.time.{Instant, ZoneId}

/** Represents an AMP HTTP measurement, as well as the metadata associated with
  * the scheduled test that generated it.
  *
  * @see [[HTTP]]
  * @see [[HTTPMeta]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-http]]
  */
case class RichHTTP(
  stream                               : String,
  source                               : String,
  destination                          : String,
  max_connections                      : Int,
  max_connections_per_server           : Int,
  max_persistent_connections_per_server: Int,
  pipelining_max_requests              : Int,
  persist                              : Boolean,
  pipelining                           : Boolean,
  caching                              : Boolean,
  bytes                                : Option[Int],
  duration                             : Option[Int],
  object_count                         : Int,
  server_count                         : Int,
  time                                 : Instant
) extends RichInfluxMeasurement {
  override def toString: String = {
    s"${HTTP.table_name}," +
      s"stream=$stream " +
      s"source=$source," +
      s"destination=$destination," +
      s"max_connections=$max_connections," +
      s"max_connections_per_server=$max_connections_per_server," +
      s"max_persistent_connections_per_server=$max_persistent_connections_per_server," +
      s"pipelining_max_requests=$pipelining_max_requests," +
      s"persist=$persist," +
      s"pipelining=$pipelining," +
      s"caching=$caching," +
      s"bytes=${bytes.getOrElse("")}," +
      s"duration=${duration.getOrElse("")}," +
      s"object_count=$object_count," +
      s"server_count=$server_count " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def isLossy: Boolean = bytes.isEmpty

  override def toCsvFormat: Seq[String] = RichHTTP.unapply(this).get.productIterator.toSeq.map(toCsvEntry)

  var defaultValue: Option[Double] = bytes.map(_.toDouble)
}

object RichHTTP extends RichInfluxMeasurementFactory {
  override def create(base: InfluxMeasurement, meta: PostgresMeasurementMeta): Option[RichInfluxMeasurement] = {
    base match {
      case b: HTTP =>
        meta match {
          case m: HTTPMeta =>
            Some(
              RichHTTP(
                m.stream.toString,
                m.source,
                m.destination,
                m.max_connections,
                m.max_connections_per_server,
                m.max_persistent_connections_per_server,
                m.pipelining_max_requests,
                m.persist,
                m.pipelining,
                m.caching,
                b.bytes,
                b.duration,
                b.object_count,
                b.server_count,
                b.time
              ))
          case _ => None
        }
      case _ => None
    }
  }
}
