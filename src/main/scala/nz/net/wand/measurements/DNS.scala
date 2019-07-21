package nz.net.wand.measurements

import com.github.fsanaulla.chronicler.macros.annotations.reader.utc
import com.github.fsanaulla.chronicler.macros.annotations.{field, tag, timestamp}

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
) extends Measurement {}

object DNS extends MeasurementFactory {

  final override val table_name: String = "data_amp_dns"

  override def Create(subscriptionLine: String): Option[DNS] = {
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
          getNamedField(namedData, "opcode").toInt,
          getNamedField(namedData, "query_len").toInt,
          getNamedField(namedData, "rcode").toInt,
          getNamedField(namedData, "requests").toInt,
          getNamedField(namedData, "response_size").toInt,
          getNamedField(namedData, "rtt").toInt,
          getNamedField(namedData, "total_additional").toInt,
          getNamedField(namedData, "total_answer").toInt,
          getNamedField(namedData, "total_authority").toInt,
          getNamedField(namedData, "ttl").toInt,
          data.last.toLong
        ))
    }
  }
}
