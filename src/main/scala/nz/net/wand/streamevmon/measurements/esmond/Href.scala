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

package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.HrefTimeSeriesEntry
import nz.net.wand.streamevmon.measurements.traits.CsvOutputable

import java.time.Instant

/** Just a link to another API endpoint. */
case class Href(
  stream: String,
  hrefLocation: Option[String],
  time        : Instant
) extends EsmondMeasurement
          with CsvOutputable {
  override def toCsvFormat: Seq[String] = Seq(stream, hrefLocation, time).map(toCsvEntry)
}

object Href {
  def apply(
    stream: String,
    entry : HrefTimeSeriesEntry
  ): Href = new Href(
    stream,
    entry.hrefLocation,
    Instant.ofEpochSecond(entry.timestamp)
  )
}
