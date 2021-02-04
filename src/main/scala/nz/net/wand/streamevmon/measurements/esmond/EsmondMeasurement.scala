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

import nz.net.wand.streamevmon.connectors.esmond.schema._
import nz.net.wand.streamevmon.measurements.traits.Measurement

/** Parent class for measurements from perfSONAR esmond. These don't necessarily
  * implement either HasDefault or CsvOutputable, but we'll define a default
  * isLossy for those that can't be.
  */
trait EsmondMeasurement extends Measurement {
  override def isLossy: Boolean = false
}

/** Esmond measurements can be constructed from
  * [[nz.net.wand.streamevmon.connectors.esmond.schema schema objects]].
  */
object EsmondMeasurement {

  /** Stream IDs are derived directly from the test schedule's unique ID. */
  def calculateStreamId(eventType: EventType): String = eventType.baseUri

  /** Stream IDs are derived directly from the test schedule's unique ID. */
  def calculateStreamId(summary: Summary): String = summary.uri

  /** The apply methods of this obejct will return the correct type of
    * measurement according to the entry passed to it.
    */
  def apply(
    stream: String,
    entry : AbstractTimeSeriesEntry
  ): EsmondMeasurement = {
    entry match {
      case e: HistogramTimeSeriesEntry => Histogram(stream, e)
      case e: HrefTimeSeriesEntry => Href(stream, e)
      case e: PacketTraceTimeSeriesEntry => PacketTrace(stream, e)
      case e: SimpleTimeSeriesEntry => Simple(stream, e)
      case e: SubintervalTimeSeriesEntry => Subinterval(stream, e)
      case e: FailureTimeSeriesEntry => Failure(stream, e)
    }
  }

  def apply(
    eventType: EventType,
    entry: AbstractTimeSeriesEntry
  ): EsmondMeasurement = apply(calculateStreamId(eventType), entry)

  def apply(
    summary: Summary,
    entry: AbstractTimeSeriesEntry
  ): EsmondMeasurement = apply(calculateStreamId(summary), entry)
}
