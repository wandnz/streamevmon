package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.measurements.{Measurement, MeasurementFactory}

import java.time.{Instant, ZoneId}
import java.util.concurrent.TimeUnit

/** Represents an AMP HTTP measurement.
  *
  * @see [[HTTPMeta]]
  * @see [[RichHTTP]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-http]]
  */
final case class HTTP(
    stream: Int,
    bytes: Int,
    duration: Int,
    object_count: Int,
    server_count: Int,
    time: Instant
) extends Measurement {
  override def toString: String = {
    s"${HTTP.table_name}," +
      s"stream=$stream " +
      s"bytes=${bytes}i," +
      s"duration=${duration}i," +
      s"object_count=${object_count}i," +
      s"server_count=${server_count}i " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def isLossy: Boolean = false
}

object HTTP extends MeasurementFactory {

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
          getNamedField(data, "stream").get.toInt,
          getNamedField(data, "bytes").get.dropRight(1).toInt,
          getNamedField(data, "duration").get.dropRight(1).toInt,
          getNamedField(data, "object_count").get.dropRight(1).toInt,
          getNamedField(data, "server_count").get.dropRight(1).toInt,
          Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(data.last.toLong))
        ))
    }
  }
}
