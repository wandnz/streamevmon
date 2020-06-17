package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema._
import nz.net.wand.streamevmon.measurements.Measurement

trait EsmondMeasurement extends Measurement {
  override def isLossy: Boolean = false
}

object EsmondMeasurement {
  def calculateStreamId(eventType: EventType): Int = eventType.baseUri.hashCode

  def calculateStreamId(summary: Summary): Int = summary.uri.hashCode

  def apply(
    stream: Int,
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
