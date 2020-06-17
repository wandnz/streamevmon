package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.HrefTimeSeriesEntry
import nz.net.wand.streamevmon.measurements.CsvOutputable

import java.time.Instant

case class RichHref(
  stream       : Int,
  hrefLocation : Option[String],
  metadataKey  : String,
  eventType    : String,
  summaryType  : Option[String],
  summaryWindow: Option[Long],
  time         : Instant
) extends RichEsmondMeasurement
          with CsvOutputable {
  override def toCsvFormat: Seq[String] = Seq(stream, hrefLocation, metadataKey, eventType, summaryType, summaryWindow, time).map(toCsvEntry)
}

object RichHref {
  def apply(
    stream       : Int,
    entry        : HrefTimeSeriesEntry,
    metadataKey  : String,
    eventType    : String,
    summaryType  : Option[String],
    summaryWindow: Option[Long]
  ): RichHref = new RichHref(
    stream,
    entry.hrefLocation,
    metadataKey,
    eventType,
    summaryType,
    summaryWindow,
    Instant.ofEpochSecond(entry.timestamp)
  )

  def apply(
    entry        : Href,
    metadataKey  : String,
    eventType    : String,
    summaryType  : Option[String],
    summaryWindow: Option[Long]
  ): RichHref = new RichHref(
    entry.stream,
    entry.hrefLocation,
    metadataKey,
    eventType,
    summaryType,
    summaryWindow,
    entry.time
  )
}
