package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema._
import nz.net.wand.streamevmon.measurements.Measurement

/** Parent class for measurements from perfSONAR esmond. These don't necessarily
  * implement either HasDefault or CsvOutputable, but we'll define a default
  * isLossy for those that can't be.
  */
trait EsmondMeasurement extends Measurement {
  override def isLossy: Boolean = false
}

/** Esmond measurements can be constructed from
  * [[nz.net.wand.streamevmon.connectors.esmond.schema schema objects]].
  */
object EsmondMeasurement {

  /** Stream IDs are derived directly from the test schedule's unique ID. */
  def calculateStreamId(eventType: EventType): String = eventType.baseUri

  /** Stream IDs are derived directly from the test schedule's unique ID. */
  def calculateStreamId(summary: Summary): String = summary.uri

  /** The apply methods of this obejct will return the correct type of
    * measurement according to the entry passed to it.
    */
  def apply(
    stream: String,
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
