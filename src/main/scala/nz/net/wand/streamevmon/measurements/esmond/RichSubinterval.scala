package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.{SubintervalTimeSeriesEntry, SubintervalValue}

import java.time.Instant

case class RichSubinterval(
  stream       : Int,
  value        : Iterable[SubintervalValue],
  metadataKey  : String,
  eventType    : String,
  summaryType  : Option[String],
  summaryWindow: Option[Long],
  time         : Instant
) extends RichEsmondMeasurement

object RichSubinterval {
  def apply(
    stream       : Int,
    entry        : SubintervalTimeSeriesEntry,
    metadataKey  : String,
    eventType    : String,
    summaryType  : Option[String],
    summaryWindow: Option[Long]
  ): RichSubinterval = new RichSubinterval(
    stream,
    entry.value,
    metadataKey,
    eventType,
    summaryType,
    summaryWindow,
    Instant.ofEpochSecond(entry.timestamp)
  )

  def apply(
    entry        : Subinterval,
    metadataKey  : String,
    eventType    : String,
    summaryType  : Option[String],
    summaryWindow: Option[Long]
  ): RichSubinterval = new RichSubinterval(
    entry.stream,
    entry.value,
    metadataKey,
    eventType,
    summaryType,
    summaryWindow,
    entry.time
  )
}
