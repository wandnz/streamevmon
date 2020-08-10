package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.{PacketTraceEntry, PacketTraceTimeSeriesEntry}

import java.time.Instant

/** Contains a list of packet trace entries showing hops on the path from one
  * host to another.
  */
case class PacketTrace(
  stream: String,
  value : Iterable[PacketTraceEntry],
  time  : Instant
) extends EsmondMeasurement {}

object PacketTrace {
  def apply(
    stream: String,
    entry : PacketTraceTimeSeriesEntry
  ): PacketTrace = new PacketTrace(
    stream,
    entry.value,
    Instant.ofEpochSecond(entry.timestamp)
  )
}
