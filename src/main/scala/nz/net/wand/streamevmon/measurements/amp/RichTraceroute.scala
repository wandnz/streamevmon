package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.measurements.{CsvOutputable, RichMeasurement}
import nz.net.wand.streamevmon.Logging

import java.time.Instant

/** Represents an AMP Traceroute measurement, as well as the metadata associated
  * with the scheduled test that generated it.
  *
  * @see [[Traceroute]]
  * @see [[TracerouteMeta]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-trace]]
  */
final case class RichTraceroute(
  stream: String,
  source: String,
  destination: String,
  family: String,
  packet_size_selection: String,
  path_id: Int,
  aspath_id: Option[Int],
  packet_size: Int,
  error_type: Option[Int],
  error_code: Option[Int],
  hop_rtt: Array[Int],
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
    Seq(stream, path_id, aspath_id, packet_size, error_type, error_code, hop_rtt)
      .map(toCsvEntry)
}

object RichTraceroute {
  def create(base: Traceroute, meta: TracerouteMeta): RichTraceroute = {
    RichTraceroute(
      base.stream,
      meta.source,
      meta.destination,
      meta.family,
      meta.packet_size_selection,
      base.path_id,
      base.aspath_id,
      base.packet_size,
      base.error_type,
      base.error_code,
      base.hop_rtt,
      base.time
    )
  }
}
