package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.HrefTimeSeriesEntry
import nz.net.wand.streamevmon.measurements.traits.CsvOutputable

import java.time.Instant

/** Just a link to another API endpoint. */
case class Href(
  stream: String,
  hrefLocation: Option[String],
  time        : Instant
) extends EsmondMeasurement
          with CsvOutputable {
  override def toCsvFormat: Seq[String] = Seq(stream, hrefLocation, time).map(toCsvEntry)
}

object Href {
  def apply(
    stream: String,
    entry : HrefTimeSeriesEntry
  ): Href = new Href(
    stream,
    entry.hrefLocation,
    Instant.ofEpochSecond(entry.timestamp)
  )
}
