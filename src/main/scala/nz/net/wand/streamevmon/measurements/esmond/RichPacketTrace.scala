package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.{PacketTraceEntry, PacketTraceTimeSeriesEntry}

import java.time.Instant

/** @see [[PacketTrace]]
  * @see [[RichEsmondMeasurement]]
  */
case class RichPacketTrace(
  stream       : Int,
  value        : Iterable[PacketTraceEntry],
  metadataKey  : String,
  eventType    : String,
  summaryType  : Option[String],
  summaryWindow: Option[Long],
  time         : Instant
) extends RichEsmondMeasurement {}

object RichPacketTrace {
  def apply(
    stream       : Int,
    entry        : PacketTraceTimeSeriesEntry,
    metadataKey  : String,
    eventType    : String,
    summaryType  : Option[String],
    summaryWindow: Option[Long]
  ): RichPacketTrace = new RichPacketTrace(
    stream,
    entry.value,
    metadataKey,
    eventType,
    summaryType,
    summaryWindow,
    Instant.ofEpochSecond(entry.timestamp)
  )

  def apply(
    entry        : PacketTrace,
    metadataKey  : String,
    eventType    : String,
    summaryType  : Option[String],
    summaryWindow: Option[Long]
  ): RichPacketTrace = new RichPacketTrace(
    entry.stream,
    entry.value,
    metadataKey,
    eventType,
    summaryType,
    summaryWindow,
    entry.time
  )
}
