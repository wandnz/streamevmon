package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.measurements.{CsvOutputable, RichMeasurement}
import nz.net.wand.streamevmon.Logging

import java.time.Instant

final case class RichTraceroute(
  stream: String,
  path_id: Int,
  aspath_id: Option[Int],
  packet_size: Int,
  error_type: Option[Int],
  error_code: Option[Int],
  hop_rtt: Array[Int],
  path_length: Option[Double],
  time: Instant
) extends RichMeasurement
          with CsvOutputable
          with Logging {

  // We have no examples of lossy measurements, so we don't really know what they look like.
  // If we ever find a measurement with an error, let's make some noise.
  override def isLossy: Boolean = {
    if (error_type.isDefined || error_code.isDefined) {
      logger.error(s"Found a potentially lossy Traceroute measurement! $this")
      true
    }
    else {
      false
    }
  }

  override def toCsvFormat: Seq[String] =
    Seq(stream, path_id, aspath_id, packet_size, error_type, error_code, path_length, hop_rtt)
      .map(toCsvEntry)
}
