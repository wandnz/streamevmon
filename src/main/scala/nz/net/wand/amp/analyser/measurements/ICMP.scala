package nz.net.wand.amp.analyser.measurements

import com.github.fsanaulla.chronicler.macros.annotations.reader.utc
import com.github.fsanaulla.chronicler.macros.annotations.{field, tag, timestamp}

final case class ICMP(
    @tag stream: String,
    @field loss: Int,
    @field lossrate: Double,
    @field median: Int,
    @field packet_size: Int,
    @field results: Int,
    @field rtts: String,
    @utc @timestamp time: Long
) extends Measurement {
  override def toString: String = {
    s"${ICMP.table_name}," +
      s"stream=$stream " +
      s"loss=$loss," +
      s"lossrate=$lossrate," +
      s"median=$median," +
      s"packet_size=$packet_size," +
      s"results=$results," +
      s"rtts=$rtts " +
      s"$time"
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
          getNamedField(namedData, "stream"),
          getNamedField(namedData, "loss").dropRight(1).toInt,
          getNamedField(namedData, "lossrate").toDouble,
          getNamedField(namedData, "median").dropRight(1).toInt,
          getNamedField(namedData, "packet_size").dropRight(1).toInt,
          getNamedField(namedData, "results").dropRight(1).toInt,
          getNamedField(namedData, "rtts"),
          data.last.toLong
        ))
    }
  }
}
