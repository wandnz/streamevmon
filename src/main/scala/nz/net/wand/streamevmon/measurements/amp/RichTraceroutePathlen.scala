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

/** Represents an AMP Traceroute-Pathlen measurement, as well as the metadata
  * associated with the scheduled test that generated it.
  *
  * @see [[TraceroutePathlen]]
  * @see [[TracerouteMeta]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-trace]]
  */
case class RichTraceroutePathlen(
  stream: String,
  source: String,
  destination: String,
  family: String,
  packet_size_selection: String,
  path_length: Option[Double],
  time  : Instant
) extends RichInfluxMeasurement {

  override def toString: String = {
    s"${TraceroutePathlen.table_name}," +
      s"stream=$stream " +
      s"source=$source," +
      s"destination=$destination," +
      s"family=$family," +
      s"packet_size_selection=$packet_size_selection," +
      s"path_length=$path_length " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def isLossy: Boolean = false

  override def toCsvFormat: Seq[String] = RichTraceroutePathlen.unapply(this).get.productIterator.toSeq.map(toCsvEntry)

  var defaultValue: Option[Double] = path_length
}

object RichTraceroutePathlen extends RichInfluxMeasurementFactory {

  override def create(base: InfluxMeasurement, meta: PostgresMeasurementMeta): Option[RichInfluxMeasurement] = {
    base match {
      case b: TraceroutePathlen =>
        meta match {
          case m: TracerouteMeta =>
            Some(
              RichTraceroutePathlen(
                m.stream.toString,
                m.source,
                m.destination,
                m.family,
                m.packet_size_selection,
                b.path_length,
                b.time
              ))
          case _ => None
        }
      case _ => None
    }
  }
}
