package nz.net.wand.streamevmon.measurements

import java.time.{Instant, ZoneId}
import java.util.concurrent.TimeUnit

/** Represents an AMP Traceroute measurement.
  *
  * @see [[TracerouteMeta]]
  * @see [[RichTraceroute]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-trace]]
  */
final case class Traceroute(
    stream: Int,
    path_length: Double,
    time: Instant
) extends Measurement {
  override def toString: String = {
    s"${Traceroute.table_name}," +
      s"stream=$stream " +
      s"path_length=$path_length " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def isLossy: Boolean = false
}

object Traceroute extends MeasurementFactory {

  final override val table_name: String = "data_amp_traceroute_pathlen"

  override def columnNames: Seq[String] = getColumnNames[Traceroute]

  override def create(subscriptionLine: String): Option[Traceroute] = {
    val data = subscriptionLine.split(Array(',', ' '))
    val namedData = data.drop(1).dropRight(1)
    if (data(0) != table_name) {
      None
    }
    else {
      Some(
        Traceroute(
          getNamedField(namedData, "stream").get.toInt,
          getNamedField(namedData, "path_length").get.toDouble,
          Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(data.last.toLong))
        ))
    }
  }
}
