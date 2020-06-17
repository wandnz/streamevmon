package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.HistogramTimeSeriesEntry

import java.time.Instant

case class RichHistogram(
  stream       : Int,
  value        : Map[Double, Int],
  metadataKey  : String,
  eventType    : String,
  summaryType  : Option[String],
  summaryWindow: Option[Long],
  time         : Instant
) extends RichEsmondMeasurement {}

object RichHistogram {
  def apply(
    stream       : Int,
    entry        : HistogramTimeSeriesEntry,
    metadataKey  : String,
    eventType    : String,
    summaryType  : Option[String],
    summaryWindow: Option[Long]
  ): RichHistogram = new RichHistogram(
    stream,
    entry.value,
    metadataKey,
    eventType,
    summaryType,
    summaryWindow,
    Instant.ofEpochSecond(entry.timestamp)
  )

  def apply(
    entry        : Histogram,
    metadataKey  : String,
    eventType    : String,
    summaryType  : Option[String],
    summaryWindow: Option[Long]
  ): RichHistogram = new RichHistogram(
    entry.stream,
    entry.value,
    metadataKey,
    eventType,
    summaryType,
    summaryWindow,
    entry.time
  )
}
