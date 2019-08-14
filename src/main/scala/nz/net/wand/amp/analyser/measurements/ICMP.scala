package nz.net.wand.amp.analyser.measurements

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

  override def enrich(): Option[RichMeasurement] = {
    MeasurementFactory.enrichMeasurement(this).asInstanceOf[Option[RichICMP]]
  }
}

object ICMP extends MeasurementFactory {

  final override val table_name: String = "data_amp_icmp"

  override def create(subscriptionLine: String): Option[ICMP] = {
    val data = subscriptionLine.split(Array(',', ' '))
    val namedData = data.drop(1).dropRight(1)
    if (data(0) != table_name) {
      None
    }
    else {
      Some(
        ICMP(
          getNamedField(namedData, "stream").get.toInt,
          getNamedField(namedData, "loss").get.dropRight(1).toInt,
          getNamedField(namedData, "lossrate").get.toDouble,
          getNamedField(namedData, "median").map(_.dropRight(1).toInt),
          getNamedField(namedData, "packet_size").get.dropRight(1).toInt,
          getNamedField(namedData, "results").get.dropRight(1).toInt,
          getRtts(getNamedField(namedData, "rtts").get),
          Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(data.last.toLong))
        ))
    }
  }
}
