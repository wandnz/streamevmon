package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.{EventType, Summary, TimeSeriesEntry}
import nz.net.wand.streamevmon.measurements.Measurement

import java.time.Instant

case class EsmondMeasurement(
  stream: Int,
  value : Double,
  time  : Instant
) extends Measurement {

  override def isLossy: Boolean = false

  override def toCsvFormat: Seq[String] = Seq(stream, value, time).map(toCsvTupleEntry)

  override var defaultValue: Option[Double] = Some(value)
}

object EsmondMeasurement {
  def calculateStreamId(eventType: EventType): Int = eventType.baseUri.hashCode

  def calculateStreamId(summary: Summary): Int = summary.uri.hashCode

  def apply(
    stream: Int,
    entry: TimeSeriesEntry
  ): EsmondMeasurement = new EsmondMeasurement(
    stream,
    entry.value,
    Instant.ofEpochSecond(entry.timestamp)
  )

  def apply(
    eventType: EventType,
    entry    : TimeSeriesEntry
  ): EsmondMeasurement = apply(calculateStreamId(eventType), entry)

  def apply(
    summary: Summary,
    entry    : TimeSeriesEntry
  ): EsmondMeasurement = apply(calculateStreamId(summary), entry)
}
