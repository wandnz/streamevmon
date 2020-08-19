package nz.net.wand.streamevmon.measurements

import java.time.{Duration, Instant}

import org.apache.commons.lang3.time.DurationFormatUtils

trait CsvOutputable {
  /** @return The object represented as a Seq as it would be in a CSV - simple
    *         types only. Collections should be converted to quoted strings
    *         separated by semicolons. Empty Options should become an empty
    *         string. Items should be organised in the order that columnNames
    *         gives. There is no need to be able to construct a Measurement
    *         object from the result of this method.
    */
  def toCsvFormat: Seq[String]

  /** @return The object, simplified in the way that toCsvFormat desires. */
  protected def toCsvEntry(e: Any): String =
    e match {
      case o: Option[Any] => o.map(toCsvEntry).getOrElse("")
      case s: Seq[Any] => s.map(toCsvEntry).mkString("\"", ";", "\"")
      case i: Instant => i.toEpochMilli.toString
      case d: Duration => DurationFormatUtils.formatDuration(d.toMillis, "H:mm:ss.S")
      case _ => e.toString
    }
}
