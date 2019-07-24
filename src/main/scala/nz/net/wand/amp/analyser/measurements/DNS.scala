package nz.net.wand.amp.analyser.measurements

import com.github.fsanaulla.chronicler.macros.annotations.{field, tag, timestamp}
import com.github.fsanaulla.chronicler.macros.annotations.reader.utc

final case class DNS(
    @tag stream: String,
    @field flag_aa: Boolean,
    @field flag_ad: Boolean,
    @field flag_cd: Boolean,
    @field flag_qr: Boolean,
    @field flag_ra: Boolean,
    @field flag_rd: Boolean,
    @field flag_tc: Boolean,
    @field lossrate: Double,
    @field opcode: Int,
    @field query_len: Int,
    @field rcode: Int,
    @field requests: Int,
    @field response_size: Int,
    @field rtt: Int,
    @field total_additional: Int,
    @field total_answer: Int,
    @field total_authority: Int,
    @field ttl: Int,
    @utc @timestamp time: Long
) extends Measurement {
  override def toString: String = {
    s"${DNS.table_name}," +
      s"stream=$stream " +
      s"flag_aa=$flag_aa," +
      s"flag_ad=$flag_ad," +
      s"flag_cd=$flag_cd," +
      s"flag_qr=$flag_qr," +
      s"flag_ra=$flag_ra," +
      s"flag_rd=$flag_rd," +
      s"flag_tc=$flag_tc," +
      s"lossrate=$lossrate," +
      s"opcode=$opcode," +
      s"query_len=$query_len," +
      s"rcode=$rcode," +
      s"requests=$requests," +
      s"response_size=$response_size," +
      s"rtt=$rtt," +
      s"total_additional=$total_additional," +
      s"total_answer=$total_answer," +
      s"total_authority=$total_authority," +
      s"ttl=$ttl " +
      s"$time"
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
          getNamedField(namedData, "stream"),
          getNamedField(namedData, "flag_aa").toBoolean,
          getNamedField(namedData, "flag_ad").toBoolean,
          getNamedField(namedData, "flag_cd").toBoolean,
          getNamedField(namedData, "flag_qr").toBoolean,
          getNamedField(namedData, "flag_ra").toBoolean,
          getNamedField(namedData, "flag_rd").toBoolean,
          getNamedField(namedData, "flag_tc").toBoolean,
          getNamedField(namedData, "lossrate").toDouble,
          getNamedField(namedData, "opcode").dropRight(1).toInt,
          getNamedField(namedData, "query_len").dropRight(1).toInt,
          getNamedField(namedData, "rcode").dropRight(1).toInt,
          getNamedField(namedData, "requests").dropRight(1).toInt,
          getNamedField(namedData, "response_size").dropRight(1).toInt,
          getNamedField(namedData, "rtt").dropRight(1).toInt,
          getNamedField(namedData, "total_additional").dropRight(1).toInt,
          getNamedField(namedData, "total_answer").dropRight(1).toInt,
          getNamedField(namedData, "total_authority").dropRight(1).toInt,
          getNamedField(namedData, "ttl").dropRight(1).toInt,
          data.last.toLong
        ))
    }
  }
}
