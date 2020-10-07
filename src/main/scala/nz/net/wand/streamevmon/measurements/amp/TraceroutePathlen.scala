package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.measurements.{InfluxMeasurement, InfluxMeasurementFactory}

import java.time.{Instant, ZoneId}
import java.util.concurrent.TimeUnit

/** Represents an AMP Traceroute-Pathlen measurement.
  *
  * @see [[TracerouteMeta]]
  * @see [[RichTraceroutePathlen]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-trace]]
  */
final case class TraceroutePathlen(
  stream: String,
  path_length: Option[Double],
  time: Instant
) extends InfluxMeasurement {
  override def toString: String = {
    s"${TraceroutePathlen.table_name}," +
      s"stream=$stream " +
      s"path_length=$path_length " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def isLossy: Boolean = false

  override def toCsvFormat: Seq[String] = TraceroutePathlen.unapply(this).get.productIterator.toSeq.map(toCsvEntry)

  var defaultValue: Option[Double] = path_length
}

object TraceroutePathlen extends InfluxMeasurementFactory {

  final override val table_name: String = "data_amp_traceroute_pathlen"

  override def columnNames: Seq[String] = getColumnNames[TraceroutePathlen]

  override def create(subscriptionLine: String): Option[TraceroutePathlen] = {
    val data = splitLineProtocol(subscriptionLine)
    if (data.head != table_name) {
      None
    }
    else {
      Some(
        TraceroutePathlen(
          getNamedField(data, "stream").get,
          getNamedField(data, "path_length").map(_.toDouble),
          Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(data.last.toLong))
        ))
    }
  }
}
