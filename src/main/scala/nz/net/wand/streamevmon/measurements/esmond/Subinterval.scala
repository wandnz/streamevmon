package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.{SubintervalTimeSeriesEntry, SubintervalValue}

import java.time.Instant

/** Subinterval-type measurements contain detail about values at fine-grained
  * time intervals.
  */
case class Subinterval(
  stream: String,
  value : Iterable[SubintervalValue],
  time  : Instant
) extends EsmondMeasurement

object Subinterval {
  def apply(
    stream: String,
    entry : SubintervalTimeSeriesEntry
  ): Subinterval = new Subinterval(
    stream,
    entry.value,
    Instant.ofEpochSecond(entry.timestamp)
  )
}
