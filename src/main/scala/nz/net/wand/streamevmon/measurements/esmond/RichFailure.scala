package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.FailureTimeSeriesEntry
import nz.net.wand.streamevmon.measurements.traits.CsvOutputable

import java.time.Instant

/** @see [[Failure]]
  * @see [[RichEsmondMeasurement]]
  */
case class RichFailure(
  stream: String,
  failureText  : Option[String],
  metadataKey  : String,
  eventType    : String,
  summaryType  : Option[String],
  summaryWindow: Option[Long],
  time         : Instant
) extends RichEsmondMeasurement
          with CsvOutputable {
  override def toCsvFormat: Seq[String] =
    Seq(stream, failureText, metadataKey, eventType, summaryType, summaryWindow, time).map(toCsvEntry)
}

object RichFailure {
  def apply(
    stream: String,
    entry        : FailureTimeSeriesEntry,
    metadataKey  : String,
    eventType    : String,
    summaryType  : Option[String],
    summaryWindow: Option[Long]
  ): RichFailure = new RichFailure(
    stream,
    entry.failureText,
    metadataKey,
    eventType,
    summaryType,
    summaryWindow,
    Instant.ofEpochSecond(entry.timestamp)
  )

  def apply(
    entry        : Failure,
    metadataKey  : String,
    eventType    : String,
    summaryType  : Option[String],
    summaryWindow: Option[Long]
  ): RichFailure = RichFailure(
    entry.stream,
    entry.failureText,
    metadataKey,
    eventType,
    summaryType,
    summaryWindow,
    entry.time
  )
}
