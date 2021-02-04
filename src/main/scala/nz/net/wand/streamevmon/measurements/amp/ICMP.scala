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

/** Represents an AMP ICMP measurement.
  *
  * @see [[ICMPMeta]]
  * @see [[RichICMP]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-icmp]]
  */
final case class ICMP(
  stream     : String,
  loss       : Option[Int],
  lossrate   : Option[Double],
  median     : Option[Int],
  packet_size: Int,
  results    : Option[Int],
  rtts       : Seq[Option[Int]],
  time       : Instant
) extends InfluxMeasurement {
  override def toString: String = {
    s"${ICMP.table_name}," +
      s"stream=$stream " +
      s"loss=$loss," +
      s"lossrate=$lossrate," +
      s"median=${median.getOrElse("")}," +
      s"packet_size=$packet_size," +
      s"results=$results," +
      s"rtts=${rtts.map(x => x.getOrElse("None")).mkString("\"[", ",", "]\"")} " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def isLossy: Boolean = loss.getOrElse(100) > 0

  override def toCsvFormat: Seq[String] = ICMP.unapply(this).get.productIterator.toSeq.map(toCsvEntry)

  var defaultValue: Option[Double] = median.map(_.toDouble)
}

object ICMP extends InfluxMeasurementFactory {

  final override val table_name: String = "data_amp_icmp"

  override def columnNames: Seq[String] = getColumnNames[ICMP]

  def apply(
    stream     : String,
    loss       : Option[Int],
    lossrate   : Option[Double],
    median     : Option[Int],
    packet_size: Int,
    results    : Option[Int],
    rtts       : String,
    time       : Instant
  ): ICMP =
    new ICMP(
      stream,
      loss,
      lossrate,
      median,
      packet_size,
      results,
      getRtts(rtts),
      time
    )

  override def create(subscriptionLine: String): Option[ICMP] = {
    val data = splitLineProtocol(subscriptionLine)
    if (data.head != table_name) {
      None
    }
    else {
      Some(
        ICMP(
          getNamedField(data, "stream").get,
          getNamedField(data, "loss").map(_.dropRight(1).toInt),
          getNamedField(data, "lossrate").map(_.toDouble),
          getNamedField(data, "median").map(_.dropRight(1).toInt),
          getNamedField(data, "packet_size").get.dropRight(1).toInt,
          getNamedField(data, "results").map(_.dropRight(1).toInt),
          getRtts(getNamedField(data, "rtts").get),
          Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(data.last.toLong))
        ))
    }
  }
}
