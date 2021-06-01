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

import java.time.Instant

case class Udpstream(
  source: String,
  destination: String,
  test  : String,
  time: Instant,
  direction: Direction,
  dscp: String,
  family: String,
  packet_count: Long,
  packet_size: Long,
  packet_spacing: Long,
  count : Option[Long],
  jitter: Option[Long],
  loss  : Option[Double],
  mos   : Option[Double],
  rtt   : Option[Long]
) extends Amp2Measurement {
  override val measurementName: String = Udpstream.measurementName
  override val tags: Seq[Any] = Seq(direction, dscp, family, packet_count, packet_size, packet_spacing)

  override var defaultValue: Option[Double] = rtt.map(_.toDouble)
}

object Udpstream {
  val measurementName = "udpstream"

  /** @see [[Amp2Measurement `Amp2Measurement.createFromLineProtocol`]] */
  def create(proto: LineProtocol): Option[Udpstream] = {
    if (proto.measurementName != measurementName) {
      None
    }
    else {
      Some(Udpstream(
        proto.tags("source"),
        proto.tags("destination"),
        proto.tags("test"),
        proto.time,
        proto.getTagAsDirection("direction"),
        proto.tags("dscp"),
        proto.tags("family"),
        proto.getTagAsLong("packet_count"),
        proto.getTagAsLong("packet_size"),
        proto.getTagAsLong("packet_spacing"),
        proto.getFieldAsLong("count"),
        proto.getFieldAsLong("jitter"),
        proto.getFieldAsDouble("loss"),
        proto.getFieldAsDouble("mos"),
        proto.getFieldAsLong("rtt")
      ))
    }
  }
}
