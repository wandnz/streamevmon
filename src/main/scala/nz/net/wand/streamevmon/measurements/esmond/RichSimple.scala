package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema._
import nz.net.wand.streamevmon.measurements.traits.{CsvOutputable, HasDefault}

import java.time.Instant

/** @see [[Simple]]
  * @see [[RichEsmondMeasurement]]
  */
case class RichSimple(
  stream: String,
  value        : Double,
  metadataKey  : String,
  eventType    : String,
  summaryType  : Option[String],
  summaryWindow: Option[Long],
  time         : Instant
) extends RichEsmondMeasurement
          with CsvOutputable
          with HasDefault {
  override def toCsvFormat: Seq[String] =
    Seq(stream, value, metadataKey, eventType, summaryType, summaryWindow, time).map(toCsvEntry)

  override var defaultValue: Option[Double] = Some(value)
}

object RichSimple {
  def apply(
    stream: String,
    entry        : SimpleTimeSeriesEntry,
    metadataKey  : String,
    eventType    : String,
    summaryType  : Option[String],
    summaryWindow: Option[Long]
  ): RichSimple = new RichSimple(
    stream,
    entry.value,
    metadataKey,
    eventType,
    summaryType,
    summaryWindow,
    Instant.ofEpochSecond(entry.timestamp)
  )

  def apply(
    entry        : Simple,
    metadataKey  : String,
    eventType    : String,
    summaryType  : Option[String],
    summaryWindow: Option[Long]
  ): RichSimple = new RichSimple(
    entry.stream,
    entry.value,
    metadataKey,
    eventType,
    summaryType,
    summaryWindow,
    entry.time
  )
}
