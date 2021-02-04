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

package nz.net.wand.streamevmon.measurements.latencyts

import nz.net.wand.streamevmon.measurements.traits.{CsvOutputable, HasDefault, RichMeasurement}

import java.time.Instant

/** Represents an AMP ICMP measurement, but only containing the data contained
  * in the Latency TS I dataset. Comparable to a RichICMP object, but missing
  * some fields.
  *
  * @see [[nz.net.wand.streamevmon.measurements.amp.RichICMP RichICMP]]
  * @see [[nz.net.wand.streamevmon.flink.sources.LatencyTSAmpFileInputFormat LatencyTSAmpFileInputFormat]]
  * @see [[LatencyTSSmokeping]]
  * @see [[https://wand.net.nz/wits/latency/1/]]
  */
case class LatencyTSAmpICMP(
  stream: String,
  source  : String,
  destination: String,
  family: String,
  time: Instant,
  average: Int,
  lossrate: Double
) extends RichMeasurement with CsvOutputable with HasDefault {
  override def toString: String = {
    f"$source-$destination-$family,${time.getEpochSecond.toInt},$average,$lossrate%.3f"
  }

  override def isLossy: Boolean = lossrate > 0.0

  override def toCsvFormat: Seq[String] = LatencyTSAmpICMP.unapply(this).get.productIterator.toSeq.map(toCsvEntry)

  var defaultValue: Option[Double] = Some(average)
}

object LatencyTSAmpICMP {
  val packet_size = 84

  def create(line: String, streamId: String): LatencyTSAmpICMP = {
    val fields = line.split(',')
    val meta = fields(0).split('-')

    LatencyTSAmpICMP(
      streamId,
      meta(0),
      meta(1),
      meta(2),
      Instant.ofEpochSecond(fields(1).toLong),
      fields(2).toInt,
      fields(3).toDouble
    )
  }
}
