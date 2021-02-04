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

import nz.net.wand.streamevmon.measurements.traits.{InfluxMeasurement, InfluxMeasurementFactory}

import java.time.{Instant, ZoneId}
import java.util.concurrent.TimeUnit

/** Represents an AMP Traceroute-Pathlen measurement.
  *
  * @see [[TracerouteMeta]]
  * @see [[RichTraceroutePathlen]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-trace]]
  */
final case class TraceroutePathlen(
  stream: String,
  path_length: Option[Double],
  time: Instant
) extends InfluxMeasurement {
  override def toString: String = {
    s"${TraceroutePathlen.table_name}," +
      s"stream=$stream " +
      s"path_length=$path_length " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def isLossy: Boolean = false

  override def toCsvFormat: Seq[String] = TraceroutePathlen.unapply(this).get.productIterator.toSeq.map(toCsvEntry)

  var defaultValue: Option[Double] = path_length
}

object TraceroutePathlen extends InfluxMeasurementFactory {

  final override val table_name: String = "data_amp_traceroute_pathlen"

  override def columnNames: Seq[String] = getColumnNames[TraceroutePathlen]

  override def create(subscriptionLine: String): Option[TraceroutePathlen] = {
    val data = splitLineProtocol(subscriptionLine)
    if (data.head != table_name) {
      None
    }
    else {
      Some(
        TraceroutePathlen(
          getNamedField(data, "stream").get,
          getNamedField(data, "path_length").map(_.toDouble),
          Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(data.last.toLong))
        ))
    }
  }
}
