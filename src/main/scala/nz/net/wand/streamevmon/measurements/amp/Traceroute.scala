package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.measurements.{CsvOutputable, Measurement}
import nz.net.wand.streamevmon.Logging

import java.time.Instant

import org.squeryl.annotations.Column

/** Represents an AMP Traceroute measurement.
  *
  * @see [[TracerouteMeta]]
  * @see [[RichTraceroute]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-trace]]
  */
case class Traceroute(
  @Column("stream_id")
  stream     : String,
  path_id    : Int,
  aspath_id  : Option[Int],
  packet_size: Int,
  error_type : Option[Int],
  error_code : Option[Int],
  @Column("hop_rtt")
  raw_rtts   : Array[Int],
  timestamp  : Int
) extends Measurement
          with CsvOutputable
          with Logging {

  lazy val time: Instant = Instant.ofEpochSecond(timestamp)

  lazy val rtts: Array[Option[Int]] = raw_rtts.map { case 0 => None; case r => Some(r) }

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
    Seq(stream, path_id, aspath_id, packet_size, error_type, error_code, rtts, time)
      .map(toCsvEntry)

  def canEqual(other: Any): Boolean = other.isInstanceOf[Traceroute]

  override def equals(other: Any): Boolean = other match {
    case that: Traceroute =>
      (that canEqual this) &&
        stream == that.stream &&
        path_id == that.path_id &&
        aspath_id == that.aspath_id &&
        packet_size == that.packet_size &&
        error_type == that.error_type &&
        error_code == that.error_code &&
        raw_rtts.sameElements(raw_rtts) &&
        timestamp == that.timestamp
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(stream, path_id, aspath_id, packet_size, error_type, error_code, raw_rtts, timestamp)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
