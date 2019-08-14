package nz.net.wand.amp.analyser.measurements

import java.time.{Instant, ZoneId}
import java.util.concurrent.TimeUnit

/** Represents an AMP DNS measurement.
  *
  * @see [[DNSMeta]]
  * @see [[RichDNS]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-dns]]
  */
final case class DNS(
    stream: Int,
    flag_aa: Option[Boolean],
    flag_ad: Option[Boolean],
    flag_cd: Option[Boolean],
    flag_qr: Option[Boolean],
    flag_ra: Option[Boolean],
    flag_rd: Option[Boolean],
    flag_tc: Option[Boolean],
    lossrate: Double,
    opcode: Option[Int],
    query_len: Int,
    rcode: Option[Int],
    requests: Int,
    response_size: Option[Int],
    rtt: Option[Int],
    total_additional: Option[Int],
    total_answer: Option[Int],
    total_authority: Option[Int],
    ttl: Option[Int],
    time: Instant
) extends Measurement {
  override def toString: String = {
    s"${DNS.table_name}," +
      s"stream=$stream " +
      s"flag_aa=${flag_aa.getOrElse("")}," +
      s"flag_ad=${flag_ad.getOrElse("")}," +
      s"flag_cd=${flag_cd.getOrElse("")}," +
      s"flag_qr=${flag_qr.getOrElse("")}," +
      s"flag_ra=${flag_ra.getOrElse("")}," +
      s"flag_rd=${flag_rd.getOrElse("")}," +
      s"flag_tc=${flag_tc.getOrElse("")}," +
      s"lossrate=$lossrate," +
      s"opcode=${opcode.map(x => s"${x}i").getOrElse("")}," +
      s"query_len=${query_len}i," +
      s"rcode=${rcode.map(x => s"${x}i").getOrElse("")}," +
      s"requests=${requests}i," +
      s"response_size=${response_size.map(x => s"${x}i").getOrElse("")}," +
      s"rtt=${rtt.map(x => s"${x}i").getOrElse("")}," +
      s"total_additional=${total_additional.map(x => s"${x}i").getOrElse("")}," +
      s"total_answer=${total_answer.map(x => s"${x}i").getOrElse("")}," +
      s"total_authority=${total_authority.map(x => s"${x}i").getOrElse("")}," +
      s"ttl=${ttl.map(x => s"${x}i").getOrElse("")} " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def enrich(): Option[RichDNS] = {
    MeasurementFactory.enrichMeasurement(this).asInstanceOf[Option[RichDNS]]
  }
}

object DNS extends MeasurementFactory {

  final override val table_name: String = "data_amp_dns"

  override def create(subscriptionLine: String): Option[DNS] = {
    val data = subscriptionLine.split(Array(',', ' '))
    val namedData = data.drop(1).dropRight(1)
    if (data(0) != table_name) {
      None
    }
    else {
      Some(
        DNS(
          getNamedField(namedData, "stream").get.toInt,
          getNamedField(namedData, "flag_aa").map(_.toBoolean),
          getNamedField(namedData, "flag_ad").map(_.toBoolean),
          getNamedField(namedData, "flag_cd").map(_.toBoolean),
          getNamedField(namedData, "flag_qr").map(_.toBoolean),
          getNamedField(namedData, "flag_ra").map(_.toBoolean),
          getNamedField(namedData, "flag_rd").map(_.toBoolean),
          getNamedField(namedData, "flag_tc").map(_.toBoolean),
          getNamedField(namedData, "lossrate").get.toDouble,
          getNamedField(namedData, "opcode").map(_.dropRight(1).toInt),
          getNamedField(namedData, "query_len").get.dropRight(1).toInt,
          getNamedField(namedData, "rcode").map(_.dropRight(1).toInt),
          getNamedField(namedData, "requests").get.dropRight(1).toInt,
          getNamedField(namedData, "response_size").map(_.dropRight(1).toInt),
          getNamedField(namedData, "rtt").map(_.dropRight(1).toInt),
          getNamedField(namedData, "total_additional").map(_.dropRight(1).toInt),
          getNamedField(namedData, "total_answer").map(_.dropRight(1).toInt),
          getNamedField(namedData, "total_authority").map(_.dropRight(1).toInt),
          getNamedField(namedData, "ttl").map(_.dropRight(1).toInt),
          Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(data.last.toLong))
        ))
    }
  }
}
