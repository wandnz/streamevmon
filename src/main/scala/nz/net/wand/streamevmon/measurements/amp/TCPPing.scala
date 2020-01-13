package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.measurements.{Measurement, MeasurementFactory}

import java.time.{Instant, ZoneId}
import java.util.concurrent.TimeUnit

/** Represents an AMP TCPPing measurement.
  *
  * @see [[TCPPingMeta]]
  * @see [[RichTCPPing]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-tcpping]]
  */
final case class TCPPing(
  stream: Int,
  icmperrors: Option[Int],
  loss: Int,
  lossrate: Double,
  median: Option[Int],
  packet_size: Int,
  results: Int,
  rtts: Seq[Option[Int]],
  time: Instant
) extends Measurement {
  override def toString: String = {
    s"${TCPPing.table_name}," +
      s"stream=$stream " +
      s"icmperrors=${icmperrors}i," +
      s"loss=${loss}i," +
      s"lossrate=$lossrate," +
      s"median=${median.map(x => s"${x}i").getOrElse("")}," +
      s"packet_size=${packet_size}i," +
      s"results=${results}i," +
      s"rtts=${rtts.map(x => x.getOrElse("None")).mkString("\"[", ",", "]\"")} " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def isLossy: Boolean = loss > 0

  var defaultValue: Option[Double] = median.map(_.toDouble)
}

object TCPPing extends MeasurementFactory {
  final override val table_name: String = "data_amp_tcpping"

  override def columnNames: Seq[String] = getColumnNames[TCPPing]

  def apply(
    stream: Int,
    icmperrors: Option[Int],
    loss: Int,
    lossrate: Double,
    median: Option[Int],
    packet_size: Int,
    results : Int,
    rtts    : String,
    time    : Instant
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
          getNamedField(data, "stream").get.toInt,
          getNamedField(data, "icmperrors").map(_.dropRight(1).toInt),
          getNamedField(data, "loss").get.dropRight(1).toInt,
          getNamedField(data, "lossrate").get.toDouble,
          getNamedField(data, "median").map(_.dropRight(1).toInt),
          getNamedField(data, "packet_size").get.dropRight(1).toInt,
          getNamedField(data, "results").get.dropRight(1).toInt,
          getRtts(getNamedField(data, "rtts").get),
          Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(data.last.toLong))
        ))
    }
  }
}
