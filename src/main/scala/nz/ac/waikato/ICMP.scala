package nz.ac.waikato

import com.github.fsanaulla.chronicler.macros.annotations.reader.utc
import com.github.fsanaulla.chronicler.macros.annotations.{field, tag, timestamp}

final case class ICMP(
  @tag stream: String,
  @field loss: Int,
  @field lossrate: Double,
  @field median: Int,
  @field packet_size: Int,
  @field results    : Int,
  @field rtts       : String,
  @utc @timestamp time: Long
)
{
  override def toString: String =
  {
    s"data_amp_icmp," +
      s"stream=$stream " +
      s"loss=$loss," +
      s"lossrate=$lossrate," +
      s"median=$median," +
      s"packet_size=$packet_size" +
      s"results=$results," +
      s"rtts=$rtts " +
      s"$time"
  }
}

object ICMPFactory
{
  def CreateICMP (subscriptionLine: String): ICMP =
  {
    val data = subscriptionLine.split(Array(',', ' '))
    ICMP(
      data(1).split('=')(1),
      data(2).split('=')(1).replaceAll("[i]", "").toInt,
      data(3).split('=')(1).replaceAll("[i]", "").toDouble,
      data(4).split('=')(1).replaceAll("[i]", "").toInt,
      data(5).split('=')(1).replaceAll("[i]", "").toInt,
      data(6).split('=')(1).replaceAll("[i]", "").toInt,
      data(7).split('=')(1),
      data(8).toLong)
  }

  def CreateICMPs (subscriptionPacketHTTP: String): Array[ICMP] =
  {
    subscriptionPacketHTTP.split('\n')
      .zipWithIndex
      // Magic number 6 is the length of the HTTP header provided by InfluxDB
      .filter(_._2 > 6)
      .map(_._1)
      .map(x => CreateICMP(x))
  }
}