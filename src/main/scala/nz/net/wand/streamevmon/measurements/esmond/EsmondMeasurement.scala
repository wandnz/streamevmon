package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.{EventType, SimpleTimeSeriesEntry, Summary}
import nz.net.wand.streamevmon.measurements.Measurement

import java.time.Instant

case class EsmondMeasurement(
  stream: Int,
  value : Double,
  time  : Instant
) extends Measurement {
  override def isLossy: Boolean = false
}

object EsmondMeasurement {
  def calculateStreamId(eventType: EventType): Int = eventType.baseUri.hashCode

  def calculateStreamId(summary: Summary): Int = summary.uri.hashCode

  def apply(
    stream: Int,
    entry: SimpleTimeSeriesEntry
  ): EsmondMeasurement = new EsmondMeasurement(
    stream,
    entry.value,
    Instant.ofEpochSecond(entry.timestamp)
  )

  def apply(
    eventType: EventType,
    entry: SimpleTimeSeriesEntry
  ): EsmondMeasurement = apply(calculateStreamId(eventType), entry)

  def apply(
    summary: Summary,
    entry: SimpleTimeSeriesEntry
  ): EsmondMeasurement = apply(calculateStreamId(summary), entry)
}
