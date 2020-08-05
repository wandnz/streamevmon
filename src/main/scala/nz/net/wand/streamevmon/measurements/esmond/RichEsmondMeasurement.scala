package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema._
import nz.net.wand.streamevmon.measurements.RichMeasurement

/** A RichEsmondMeasurement keeps track of a few more things than a regular
  * EsmondMeasurement. In particular, the measurement stream's unique key, and
  * information about the event and summary types.
  */
trait RichEsmondMeasurement
  extends EsmondMeasurement
          with RichMeasurement {

  val metadataKey: String
  val eventType: String
  val summaryType: Option[String]
  val summaryWindow: Option[Long]

  if (summaryType.isDefined != summaryWindow.isDefined) {
    throw new IllegalArgumentException(
      "In a valid RichEsmondMeasurement, summaryType and summaryWindow must " +
        "either both be defined or both be undefined."
    )
  }
}

object RichEsmondMeasurement {

  /** The apply methods of this obejct will return the correct type of
    * measurement according to the entry passed to it.
    */
  def apply(
    stream       : Int,
    entry        : AbstractTimeSeriesEntry,
    metadataKey  : String,
    eventType    : String,
    summaryType  : Option[String],
    summaryWindow: Option[Long],
  ): RichEsmondMeasurement = {
    entry match {
      case e: HistogramTimeSeriesEntry => RichHistogram(stream, e, metadataKey, eventType, summaryType, summaryWindow)
      case e: HrefTimeSeriesEntry => RichHref(stream, e, metadataKey, eventType, summaryType, summaryWindow)
      case e: PacketTraceTimeSeriesEntry => RichPacketTrace(stream, e, metadataKey, eventType, summaryType, summaryWindow)
      case e: SimpleTimeSeriesEntry => RichSimple(stream, e, metadataKey, eventType, summaryType, summaryWindow)
      case e: SubintervalTimeSeriesEntry => RichSubinterval(stream, e, metadataKey, eventType, summaryType, summaryWindow)
      case e: FailureTimeSeriesEntry => RichFailure(stream, e, metadataKey, eventType, summaryType, summaryWindow)
    }
  }

  def apply(
    eventType: EventType,
    entry    : AbstractTimeSeriesEntry
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
    entry  : AbstractTimeSeriesEntry
  ): RichEsmondMeasurement = apply(
    EsmondMeasurement.calculateStreamId(summary),
    entry,
    summary.metadataKey,
    summary.eventType,
    Some(summary.summaryType),
    Some(summary.summaryWindow)
  )

  /** We can also enrich existing EsmondMeasurements.
    */
  def apply(
    entry        : EsmondMeasurement,
    metadataKey  : String,
    eventType    : String,
    summaryType  : Option[String],
    summaryWindow: Option[Long],
  ): RichEsmondMeasurement = {
    entry match {
      case m: Histogram => RichHistogram(m, metadataKey, eventType, summaryType, summaryWindow)
      case m: Href => RichHref(m, metadataKey, eventType, summaryType, summaryWindow)
      case m: PacketTrace => RichPacketTrace(m, metadataKey, eventType, summaryType, summaryWindow)
      case m: Simple => RichSimple(m, metadataKey, eventType, summaryType, summaryWindow)
      case m: Subinterval => RichSubinterval(m, metadataKey, eventType, summaryType, summaryWindow)
      case m: Failure => RichFailure(m, metadataKey, eventType, summaryType, summaryWindow)
    }
  }

  def apply(
    eventType  : EventType,
    measurement: EsmondMeasurement
  ): RichEsmondMeasurement = apply(
    measurement,
    eventType.metadataKey,
    eventType.eventType,
    None,
    None
  )

  def apply(
    summary    : Summary,
    measurement: EsmondMeasurement
  ): RichEsmondMeasurement = apply(
    measurement,
    summary.metadataKey,
    summary.eventType,
    Some(summary.summaryType),
    Some(summary.summaryWindow)
  )
}
