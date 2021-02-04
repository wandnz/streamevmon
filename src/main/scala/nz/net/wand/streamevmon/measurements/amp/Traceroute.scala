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

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.measurements.traits.PostgresMeasurement

import java.time.Instant

import org.squeryl.annotations.Column

/** Represents an AMP Traceroute measurement.
  *
  * @see [[TracerouteMeta]]
  * @see [[RichTraceroute]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-trace]]
  */
case class Traceroute(
  @Column("stream_id")
  stream: String,
  path_id: Int,
  aspath_id  : Option[Int],
  packet_size: Int,
  error_type : Option[Int],
  error_code : Option[Int],
  @Column("hop_rtt")
  raw_rtts   : Array[Int],
  timestamp  : Int
) extends PostgresMeasurement
          with Logging {

  lazy val time: Instant = Instant.ofEpochSecond(timestamp)

  lazy val rtts: Array[Option[Int]] = raw_rtts.map { case 0 => None; case r => Some(r) }

  // We have no examples of lossy measurements, so we don't really know what they look like.
  // If we ever find a measurement with an error, let's make some noise.
  override def isLossy: Boolean = error_type.isDefined || error_code.isDefined

  override def toCsvFormat: Seq[String] =
    Seq(stream, path_id, aspath_id, packet_size, error_type, error_code, rtts, time)
      .map(toCsvEntry)

  def canEqual(other: Any): Boolean = other.isInstanceOf[Traceroute]

  override def equals(other: Any): Boolean = other match {
    case that: Traceroute =>
      (that canEqual this) &&
        stream == that.stream &&
        path_id == that.path_id &&
        aspath_id == that.aspath_id &&
        packet_size == that.packet_size &&
        error_type == that.error_type &&
        error_code == that.error_code &&
        raw_rtts.sameElements(raw_rtts) &&
        timestamp == that.timestamp
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(stream, path_id, aspath_id, packet_size, error_type, error_code, raw_rtts, timestamp)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
