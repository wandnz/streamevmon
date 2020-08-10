package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.HistogramTimeSeriesEntry

import java.time.Instant

/** Shows the frequency of items in a number of continuous buckets. This
  * measurement shows a distribution of several more specific measurements.
  */
case class Histogram(
  stream: String,
  value : Map[Double, Int],
  time  : Instant
) extends EsmondMeasurement {}

object Histogram {
  def apply(
    stream: String,
    entry : HistogramTimeSeriesEntry
  ): Histogram = new Histogram(
    stream,
    entry.value,
    Instant.ofEpochSecond(entry.timestamp)
  )
}
