package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.{EventType, Summary, TimeSeriesEntry}
import nz.net.wand.streamevmon.measurements.RichMeasurement

import java.time.Instant

case class RichEsmondMeasurement(
  stream       : Int,
  value        : Double,
  metadataKey  : String,
  eventType    : String,
  summaryType  : Option[String],
  summaryWindow: Option[Long],
  time         : Instant
) extends RichMeasurement {

  override def isLossy: Boolean = false

  override def toCsvFormat: Seq[String] =
    Seq(stream, value, metadataKey, eventType, summaryType, summaryWindow, time).map(toCsvTupleEntry)

  override var defaultValue: Option[Double] = Some(value)
}

object RichEsmondMeasurement {
  def apply(
    stream: Int,
    entry: TimeSeriesEntry,
    metadataKey: String,
    eventType: String,
    summaryType: Option[String],
    summaryWindow: Option[Long]
  ): RichEsmondMeasurement = RichEsmondMeasurement(
    stream,
    entry.value,
    metadataKey,
    eventType,
    summaryType,
    summaryWindow,
    Instant.ofEpochSecond(entry.timestamp)
  )

  def apply(
    eventType     : EventType,
    entry         : TimeSeriesEntry
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
    entry: TimeSeriesEntry
  ): RichEsmondMeasurement = apply(
    EsmondMeasurement.calculateStreamId(summary),
    entry,
    summary.metadataKey,
    summary.eventType,
    Some(summary.summaryType),
    Some(summary.summaryWindow)
  )
}
