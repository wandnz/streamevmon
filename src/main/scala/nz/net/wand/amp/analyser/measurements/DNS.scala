package nz.net.wand.amp.analyser.measurements

import java.time.{Instant, ZoneId}
import java.util.concurrent.TimeUnit

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
      s"flag_aa=${flag_aa.get}," +
      s"flag_ad=${flag_ad.get}," +
      s"flag_cd=${flag_cd.get}," +
      s"flag_qr=${flag_qr.get}," +
      s"flag_ra=${flag_ra.get}," +
      s"flag_rd=${flag_rd.get}," +
      s"flag_tc=${flag_tc.get}," +
      s"lossrate=$lossrate," +
      s"opcode=${opcode.get}," +
      s"query_len=$query_len," +
      s"rcode=${rcode.get}," +
      s"requests=$requests," +
      s"response_size=${response_size.get}," +
      s"rtt=${rtt.get}," +
      s"total_additional=${total_additional.get}," +
      s"total_answer=${total_answer.get}," +
      s"total_authority=${total_authority.get}," +
      s"ttl=${ttl.get} " +
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
