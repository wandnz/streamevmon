package nz.net.wand.streamevmon.measurements

import java.time.{Instant, ZoneId}
import java.util.concurrent.TimeUnit

/** Represents an AMP ICMP measurement.
  *
  * @see [[ICMPMeta]]
  * @see [[RichICMP]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-icmp]]
  */
final case class ICMP(
    stream: Int,
    loss: Int,
    lossrate: Double,
    median: Option[Int],
    packet_size: Int,
    results: Int,
    rtts: Seq[Option[Int]],
    time: Instant
) extends Measurement {
  override def toString: String = {
    s"${ICMP.table_name}," +
      s"stream=$stream " +
      s"loss=${loss}i," +
      s"lossrate=$lossrate," +
      s"median=${median.map(x => s"${x}i").getOrElse("")}," +
      s"packet_size=${packet_size}i," +
      s"results=${results}i," +
      s"rtts=${rtts.map(x => x.getOrElse("None")).mkString("\"[", ",", "]\"")} " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def isLossy: Boolean = loss > 0
}

object ICMP extends MeasurementFactory {

  final override val table_name: String = "data_amp_icmp"

  override def columnNames: Seq[String] = getColumnNames[ICMP]

  def apply(
    stream: Int,
    loss: Int,
    lossrate: Double,
    median: Option[Int],
    packet_size: Int,
    results: Int,
    rtts: String,
    time: Instant
  ): ICMP =
    new ICMP(
      stream,
      loss,
      lossrate,
      median,
      packet_size,
      results,
      getRtts(rtts),
      time
    )

  override def create(subscriptionLine: String): Option[ICMP] = {
    val data = splitLineProtocol(subscriptionLine)
    if (data.head != table_name) {
      None
    }
    else {
      Some(
        ICMP(
          getNamedField(data, "stream").get.toInt,
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
