package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.HistogramTimeSeriesEntry

import java.time.Instant

case class Histogram(
  stream: Int,
  value : Map[Double, Int],
  time  : Instant
) extends EsmondMeasurement {}

object Histogram {
  def apply(
    stream: Int,
    entry : HistogramTimeSeriesEntry
  ): Histogram = new Histogram(
    stream,
    entry.value,
    Instant.ofEpochSecond(entry.timestamp)
  )
}
