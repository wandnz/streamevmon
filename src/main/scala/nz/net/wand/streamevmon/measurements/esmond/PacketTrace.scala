package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.{PacketTraceEntry, PacketTraceTimeSeriesEntry}

import java.time.Instant

case class PacketTrace(
  stream: Int,
  value : Iterable[PacketTraceEntry],
  time  : Instant
) extends EsmondMeasurement {}

object PacketTrace {
  def apply(
    stream: Int,
    entry : PacketTraceTimeSeriesEntry
  ): PacketTrace = new PacketTrace(
    stream,
    entry.value,
    Instant.ofEpochSecond(entry.timestamp)
  )
}
