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

import nz.net.wand.streamevmon.connectors.esmond.schema.SimpleTimeSeriesEntry
import nz.net.wand.streamevmon.measurements.traits.{CsvOutputable, HasDefault}

import java.time.Instant

/** This type can represent quite a few esmond event types for simple time
  * series data.
  */
case class Simple(
  stream: String,
  value : Double,
  time  : Instant
) extends EsmondMeasurement
          with HasDefault
          with CsvOutputable {
  override def toCsvFormat: Seq[String] = Seq(stream, value, time).map(toCsvEntry)

  override var defaultValue: Option[Double] = Some(value)
}

object Simple {
  def apply(
    stream: String,
    entry : SimpleTimeSeriesEntry
  ): Simple = new Simple(
    stream,
    entry.value,
    Instant.ofEpochSecond(entry.timestamp)
  )
}
