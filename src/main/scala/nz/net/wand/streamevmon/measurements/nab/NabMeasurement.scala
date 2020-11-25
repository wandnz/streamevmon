package nz.net.wand.streamevmon.measurements.nab

import nz.net.wand.streamevmon.measurements.traits.{CsvOutputable, HasDefault, Measurement}

import java.time._
import java.time.format.DateTimeFormatter

/** Represents a measurement from the NAB dataset. Just a time and a value.
  * The stream corresponds to the filename the measurement is from.
  *
  * @see [[https://github.com/numenta/NAB]]
  */
case class NabMeasurement(
  stream: String,
  value : Double,
  time  : Instant
)
  extends Measurement
          with CsvOutputable
          with HasDefault {

  override def isLossy: Boolean = false

  override var defaultValue: Option[Double] = Some(value)

  override def toCsvFormat: Seq[String] = Seq(NabMeasurement.formatter.format(time), value.toString)

  override def toString: String = toCsvFormat.mkString(",")
}

object NabMeasurement {
  lazy val formatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("uuuu-MM-dd HH:mm:ss")
    .withZone(ZoneId.of("UTC"))

  def apply(stream: String, line: String): NabMeasurement = {
    val parts = line.split(',')

    if (parts.length != 2) {
      throw new IllegalArgumentException(s"NAB measurement did not have two columns! Stream ID: $stream, line: $line")
    }

    NabMeasurement(
      stream,
      parts(1).toDouble,
      LocalDateTime.parse(parts(0), formatter)
        .atZone(ZoneOffset.UTC)
        .toInstant
    )
  }
}
