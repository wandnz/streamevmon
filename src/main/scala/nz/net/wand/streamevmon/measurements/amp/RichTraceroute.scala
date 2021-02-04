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
import nz.net.wand.streamevmon.measurements.traits.{CsvOutputable, RichMeasurement}

import java.time.Instant

/** Represents an AMP Traceroute measurement, as well as the metadata associated
  * with the scheduled test that generated it.
  *
  * @see [[Traceroute]]
  * @see [[TracerouteMeta]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-trace]]
  */
final case class RichTraceroute(
  stream               : String,
  source               : String,
  destination          : String,
  family               : String,
  packet_size_selection: String,
  path_id              : Int,
  aspath_id            : Option[Int],
  packet_size          : Int,
  error_type           : Option[Int],
  error_code           : Option[Int],
  rtts                 : Array[Option[Int]],
  time                 : Instant
) extends RichMeasurement
          with CsvOutputable
          with Logging {

  // We have no examples of lossy measurements, so we don't really know what they look like.
  // If we ever find a measurement with an error, let's make some noise.
  override def isLossy: Boolean = {
    if (error_type.isDefined || error_code.isDefined) {
      logger.error(s"Found a potentially lossy Traceroute measurement! $this")
      true
    }
    else {
      false
    }
  }

  override def toCsvFormat: Seq[String] =
    Seq(stream, source, destination, family, packet_size_selection, path_id, aspath_id, packet_size, error_type, error_code, rtts, time)
      .map(toCsvEntry)
}

object RichTraceroute {
  def create(base: Traceroute, meta: TracerouteMeta): RichTraceroute = {
    RichTraceroute(
      base.stream,
      meta.source,
      meta.destination,
      meta.family,
      meta.packet_size_selection,
      base.path_id,
      base.aspath_id,
      base.packet_size,
      base.error_type,
      base.error_code,
      base.rtts,
      base.time
    )
  }
}
