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

/** Represents an AMP ICMP measurement, as well as the metadata associated with
  * the scheduled test that generated it.
  *
  * @see [[ICMP]]
  * @see [[ICMPMeta]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-icmp]]
  */
case class RichICMP(
  stream: String,
  source               : String,
  destination          : String,
  family               : String,
  packet_size_selection: String,
  loss                 : Option[Int],
  lossrate             : Option[Double],
  median               : Option[Int],
  packet_size          : Int,
  results              : Option[Int],
  rtts                 : Seq[Option[Int]],
  time                 : Instant
) extends RichInfluxMeasurement {
  override def toString: String = {
    s"${ICMP.table_name}," +
      s"stream=$stream " +
      s"source=$source," +
      s"destination=$destination," +
      s"family=$family," +
      s"packet_size_selection=$packet_size_selection " +
      s"loss=$loss," +
      s"lossrate=$lossrate," +
      s"median=${median.getOrElse("")}," +
      s"packet_size=$packet_size," +
      s"results=$results," +
      s"rtts=${rtts.map(x => x.getOrElse("None")).mkString("\"[", ",", "]\"")} " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def isLossy: Boolean = loss.getOrElse(100) > 0

  override def toCsvFormat: Seq[String] = RichICMP.unapply(this).get.productIterator.toSeq.map(toCsvEntry)

  var defaultValue: Option[Double] = median.map(_.toDouble)
}

object RichICMP extends RichInfluxMeasurementFactory {

  override def create(base: InfluxMeasurement, meta: PostgresMeasurementMeta): Option[RichInfluxMeasurement] = {
    base match {
      case b: ICMP =>
        meta match {
          case m: ICMPMeta =>
            Some(
              RichICMP(
                m.stream.toString,
                m.source,
                m.destination,
                m.family,
                m.packet_size_selection,
                b.loss,
                b.lossrate,
                b.median,
                b.packet_size,
                b.results,
                b.rtts,
                b.time
              ))
          case _ => None
        }
      case _ => None
    }
  }
}
