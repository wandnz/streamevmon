package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.measurements.{InfluxMeasurement, InfluxMeasurementFactory}

import java.time.{Instant, ZoneId}
import java.util.concurrent.TimeUnit

/** Represents an AMP TCPPing measurement.
  *
  * @see [[TCPPingMeta]]
  * @see [[RichTCPPing]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-tcpping]]
  */
final case class TCPPing(
  stream: String,
  icmperrors: Option[Int],
  loss: Option[Int],
  lossrate  : Option[Double],
  median    : Option[Int],
  packet_size: Int,
  results   : Option[Int],
  rtts      : Seq[Option[Int]],
  time      : Instant
) extends InfluxMeasurement {
  override def toString: String = {
    s"${TCPPing.table_name}," +
      s"stream=$stream " +
      s"icmperrors=$icmperrors," +
      s"loss=$loss," +
      s"lossrate=$lossrate," +
      s"median=${median.getOrElse("")}," +
      s"packet_size=$packet_size," +
      s"results=$results," +
      s"rtts=${rtts.map(x => x.getOrElse("None")).mkString("\"[", ",", "]\"")} " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def isLossy: Boolean = loss.getOrElse(100) > 0

  override def toCsvFormat: Seq[String] = TCPPing.unapply(this).get.productIterator.toSeq.map(toCsvEntry)

  var defaultValue: Option[Double] = median.map(_.toDouble)
}

object TCPPing extends InfluxMeasurementFactory {
  final override val table_name: String = "data_amp_tcpping"

  override def columnNames: Seq[String] = getColumnNames[TCPPing]

  def apply(
    stream: String,
    icmperrors: Option[Int],
    loss: Option[Int],
    lossrate: Option[Double],
    median  : Option[Int],
    packet_size: Int,
    results    : Option[Int],
    rtts       : String,
    time       : Instant
  ): TCPPing =
    new TCPPing(
      stream,
      icmperrors,
      loss,
      lossrate,
      median,
      packet_size,
      results,
      getRtts(rtts),
      time
    )

  override def create(subscriptionLine: String): Option[TCPPing] = {
    val data = splitLineProtocol(subscriptionLine)
    if (data.head != table_name) {
      None
    }
    else {
      Some(
        TCPPing(
          getNamedField(data, "stream").get,
          getNamedField(data, "icmperrors").map(_.dropRight(1).toInt),
          getNamedField(data, "loss").map(_.dropRight(1).toInt),
          getNamedField(data, "lossrate").map(_.toDouble),
          getNamedField(data, "median").map(_.dropRight(1).toInt),
          getNamedField(data, "packet_size").get.dropRight(1).toInt,
          getNamedField(data, "results").map(_.dropRight(1).toInt),
          getRtts(getNamedField(data, "rtts").get),
          Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(data.last.toLong))
        ))
    }
  }
}
