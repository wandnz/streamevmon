package nz.net.wand.streamevmon.measurements.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.FailureTimeSeriesEntry
import nz.net.wand.streamevmon.measurements.traits.CsvOutputable

import java.time.Instant

/** Esmond can provide standalone streams of measurement failures. These just
  * include a time and a description.
  */
case class Failure(
  stream: String,
  failureText: Option[String],
  time       : Instant
) extends EsmondMeasurement
          with CsvOutputable {
  override def toCsvFormat: Seq[String] = Seq(stream, failureText, time).map(toCsvEntry)
}

object Failure {
  def apply(
    stream: String,
    entry : FailureTimeSeriesEntry
  ): Failure = new Failure(
    stream,
    entry.failureText,
    Instant.ofEpochSecond(entry.timestamp)
  )
}
