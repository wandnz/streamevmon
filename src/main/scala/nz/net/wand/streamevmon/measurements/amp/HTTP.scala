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

/** Represents an AMP HTTP measurement.
  *
  * @see [[HTTPMeta]]
  * @see [[RichHTTP]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-http]]
  */
final case class HTTP(
  stream      : String,
  bytes       : Option[Int],
  duration    : Option[Int],
  object_count: Int,
  server_count: Int,
  time        : Instant
) extends InfluxMeasurement {
  override def toString: String = {
    s"${HTTP.table_name}," +
      s"stream=$stream " +
      s"bytes=${bytes.getOrElse("")}," +
      s"duration=${duration.getOrElse("")}," +
      s"object_count=$object_count," +
      s"server_count=$server_count " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def isLossy: Boolean = bytes.isEmpty

  override def toCsvFormat: Seq[String] = HTTP.unapply(this).get.productIterator.toSeq.map(toCsvEntry)

  var defaultValue: Option[Double] = bytes.map(_.toDouble)
}

object HTTP extends InfluxMeasurementFactory {

  final override val table_name: String = "data_amp_http"

  override def columnNames: Seq[String] = getColumnNames[HTTP]

  override def create(subscriptionLine: String): Option[HTTP] = {
    val data = splitLineProtocol(subscriptionLine)
    if (data.head != table_name) {
      None
    }
    else {
      Some(
        HTTP(
          getNamedField(data, "stream").get,
          getNamedField(data, "bytes").map(_.dropRight(1).toInt),
          getNamedField(data, "duration").map(_.dropRight(1).toInt),
          getNamedField(data, "object_count").get.dropRight(1).toInt,
          getNamedField(data, "server_count").get.dropRight(1).toInt,
          Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(data.last.toLong))
        ))
    }
  }
}
