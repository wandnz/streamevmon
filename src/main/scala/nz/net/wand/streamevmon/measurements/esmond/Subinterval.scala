package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.{SubintervalTimeSeriesEntry, SubintervalValue}

import java.time.Instant

case class Subinterval(
  stream: Int,
  value : Iterable[SubintervalValue],
  time  : Instant
) extends EsmondMeasurement

object Subinterval {
  def apply(
    stream: Int,
    entry : SubintervalTimeSeriesEntry
  ): Subinterval = new Subinterval(
    stream,
    entry.value,
    Instant.ofEpochSecond(entry.timestamp)
  )
}
