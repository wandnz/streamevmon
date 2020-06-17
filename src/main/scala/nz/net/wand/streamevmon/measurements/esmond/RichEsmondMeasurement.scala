package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema._
import nz.net.wand.streamevmon.measurements.RichMeasurement

trait RichEsmondMeasurement
  extends EsmondMeasurement
          with RichMeasurement {

  val metadataKey: String
  val eventType: String
  val summaryType: Option[String]
  val summaryWindow: Option[Long]

  if (summaryType.isDefined != summaryWindow.isDefined) {
    throw new IllegalArgumentException(
      "summaryType and summaryWindow must both be defined in a valid RichEsmondMeasurement"
    )
  }

  override def isLossy: Boolean = false
}

object RichEsmondMeasurement {
  def calculateStreamId(eventType: EventType): Int = EsmondMeasurement.calculateStreamId(eventType)

  def calculateStreamId(summary: Summary): Int = EsmondMeasurement.calculateStreamId(summary)

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
    calculateStreamId(eventType),
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
    calculateStreamId(summary),
    entry,
    summary.metadataKey,
    summary.eventType,
    Some(summary.summaryType),
    Some(summary.summaryWindow)
  )

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
