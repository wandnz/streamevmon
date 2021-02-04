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
import nz.net.wand.streamevmon.measurements.traits.RichMeasurement

/** A RichEsmondMeasurement keeps track of a few more things than a regular
  * EsmondMeasurement. In particular, the measurement stream's unique key, and
  * information about the event and summary types.
  */
trait RichEsmondMeasurement
  extends EsmondMeasurement
          with RichMeasurement {

  val metadataKey: String
  val eventType: String
  val summaryType: Option[String]
  val summaryWindow: Option[Long]

  if (summaryType.isDefined != summaryWindow.isDefined) {
    throw new IllegalArgumentException(
      "In a valid RichEsmondMeasurement, summaryType and summaryWindow must " +
        "either both be defined or both be undefined."
    )
  }
}

object RichEsmondMeasurement {

  /** The apply methods of this obejct will return the correct type of
    * measurement according to the entry passed to it.
    */
  def apply(
    stream       : String,
    entry        : AbstractTimeSeriesEntry,
    metadataKey  : String,
    eventType    : String,
    summaryType  : Option[String],
    summaryWindow: Option[Long],
  ): RichEsmondMeasurement = {
    entry match {
      case e: HistogramTimeSeriesEntry => RichHistogram(stream, e, metadataKey, eventType, summaryType, summaryWindow)
      case e: HrefTimeSeriesEntry => RichHref(stream, e, metadataKey, eventType, summaryType, summaryWindow)
      case e: PacketTraceTimeSeriesEntry => RichPacketTrace(stream, e, metadataKey, eventType, summaryType, summaryWindow)
      case e: SimpleTimeSeriesEntry => RichSimple(stream, e, metadataKey, eventType, summaryType, summaryWindow)
      case e: SubintervalTimeSeriesEntry => RichSubinterval(stream, e, metadataKey, eventType, summaryType, summaryWindow)
      case e: FailureTimeSeriesEntry => RichFailure(stream, e, metadataKey, eventType, summaryType, summaryWindow)
    }
  }

  def apply(
    eventType: EventType,
    entry    : AbstractTimeSeriesEntry
  ): RichEsmondMeasurement = apply(
    EsmondMeasurement.calculateStreamId(eventType),
    entry,
    eventType.metadataKey,
    eventType.eventType,
    None,
    None
  )

  def apply(
    summary: Summary,
    entry  : AbstractTimeSeriesEntry
  ): RichEsmondMeasurement = apply(
    EsmondMeasurement.calculateStreamId(summary),
    entry,
    summary.metadataKey,
    summary.eventType,
    Some(summary.summaryType),
    Some(summary.summaryWindow)
  )

  /** We can also enrich existing EsmondMeasurements.
    */
  def apply(
    entry        : EsmondMeasurement,
    metadataKey  : String,
    eventType    : String,
    summaryType  : Option[String],
    summaryWindow: Option[Long],
  ): RichEsmondMeasurement = {
    entry match {
      case m: Histogram => RichHistogram(m, metadataKey, eventType, summaryType, summaryWindow)
      case m: Href => RichHref(m, metadataKey, eventType, summaryType, summaryWindow)
      case m: PacketTrace => RichPacketTrace(m, metadataKey, eventType, summaryType, summaryWindow)
      case m: Simple => RichSimple(m, metadataKey, eventType, summaryType, summaryWindow)
      case m: Subinterval => RichSubinterval(m, metadataKey, eventType, summaryType, summaryWindow)
      case m: Failure => RichFailure(m, metadataKey, eventType, summaryType, summaryWindow)
    }
  }

  def apply(
    eventType  : EventType,
    measurement: EsmondMeasurement
  ): RichEsmondMeasurement = apply(
    measurement,
    eventType.metadataKey,
    eventType.eventType,
    None,
    None
  )

  def apply(
    summary    : Summary,
    measurement: EsmondMeasurement
  ): RichEsmondMeasurement = apply(
    measurement,
    summary.metadataKey,
    summary.eventType,
    Some(summary.summaryType),
    Some(summary.summaryWindow)
  )
}
