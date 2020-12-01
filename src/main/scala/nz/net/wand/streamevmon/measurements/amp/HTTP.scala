package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.measurements.traits.{InfluxMeasurement, InfluxMeasurementFactory}

import java.time.{Instant, ZoneId}
import java.util.concurrent.TimeUnit

/** Represents an AMP HTTP measurement.
  *
  * @see [[HTTPMeta]]
  * @see [[RichHTTP]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-http]]
  */
final case class HTTP(
  stream      : String,
  bytes       : Option[Int],
  duration    : Option[Int],
  object_count: Int,
  server_count: Int,
  time        : Instant
) extends InfluxMeasurement {
  override def toString: String = {
    s"${HTTP.table_name}," +
      s"stream=$stream " +
      s"bytes=${bytes.getOrElse("")}," +
      s"duration=${duration.getOrElse("")}," +
      s"object_count=$object_count," +
      s"server_count=$server_count " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def isLossy: Boolean = bytes.isEmpty

  override def toCsvFormat: Seq[String] = HTTP.unapply(this).get.productIterator.toSeq.map(toCsvEntry)

  var defaultValue: Option[Double] = bytes.map(_.toDouble)
}

object HTTP extends InfluxMeasurementFactory {

  final override val table_name: String = "data_amp_http"

  override def columnNames: Seq[String] = getColumnNames[HTTP]

  override def create(subscriptionLine: String): Option[HTTP] = {
    val data = splitLineProtocol(subscriptionLine)
    if (data.head != table_name) {
      None
    }
    else {
      Some(
        HTTP(
          getNamedField(data, "stream").get,
          getNamedField(data, "bytes").map(_.dropRight(1).toInt),
          getNamedField(data, "duration").map(_.dropRight(1).toInt),
          getNamedField(data, "object_count").get.dropRight(1).toInt,
          getNamedField(data, "server_count").get.dropRight(1).toInt,
          Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(data.last.toLong))
        ))
    }
  }
}
