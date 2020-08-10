package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.measurements.{InfluxMeasurement, InfluxMeasurementFactory}

import java.time.{Instant, ZoneId}
import java.util.concurrent.TimeUnit

/** Represents an AMP Traceroute measurement.
  *
  * @see [[TracerouteMeta]]
  * @see [[RichTraceroute]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-trace]]
  */
final case class Traceroute(
  stream: String,
  path_length: Option[Double],
  time       : Instant
) extends InfluxMeasurement {
  override def toString: String = {
    s"${Traceroute.table_name}," +
      s"stream=$stream " +
      s"path_length=$path_length " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def isLossy: Boolean = false

  override def toCsvFormat: Seq[String] = Traceroute.unapply(this).get.productIterator.toSeq.map(toCsvEntry)

  var defaultValue: Option[Double] = path_length
}

object Traceroute extends InfluxMeasurementFactory {

  final override val table_name: String = "data_amp_traceroute_pathlen"

  override def columnNames: Seq[String] = getColumnNames[Traceroute]

  override def create(subscriptionLine: String): Option[Traceroute] = {
    val data = splitLineProtocol(subscriptionLine)
    if (data.head != table_name) {
      None
    }
    else {
      Some(
        Traceroute(
          getNamedField(data, "stream").get,
          getNamedField(data, "path_length").map(_.toDouble),
          Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(data.last.toLong))
        ))
    }
  }
}
