package nz.ac.waikato

import com.github.fsanaulla.chronicler.macros.annotations.reader.utc
import com.github.fsanaulla.chronicler.macros.annotations.{field, tag, timestamp}

final case class ICMPMeasurement(
  @tag stream: String,
  @field loss         : Int,
  @field lossrate: Double,
  @field median       : Int,
  @field packet_size: Int,
  @field results      : Int,
  @field rtts         : String,
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

  def CreateICMP(subscriptionLine: String): Option[ICMPMeasurement] =
  {
    val data = subscriptionLine.split(Array(',', ' '))
    if (!data(0).contains("icmp"))
    {
      None
    }
    else
    {
      Some(
        ICMPMeasurement(
          data(1).split('=')(1),
          data(2).split('=')(1).replace("i", "").toInt,
          data(3).split('=')(1).replace("i", "").toDouble,
          data(4).split('=')(1).replace("i", "").toInt,
          data(5).split('=')(1).replace("i", "").toInt,
          data(6).split('=')(1).replace("i", "").toInt,
          data(7).split('=')(1),
          data(8).toLong
        ))
    }
  }

  def CreateICMPs(subscriptionPacketHTTP: String): Array[ICMPMeasurement] =
  {
    subscriptionPacketHTTP
      .split('\n')
      // Drop the HTTP header
      .dropWhile(!_.isEmpty)
      .drop(1)
      .flatMap(x =>
      {
        CreateICMP(x)
      })
  }

  def CreateICMPs(
    subscriptionPacketHTTP: Stream[String]
  ): Stream[ICMPMeasurement] =
  {
    subscriptionPacketHTTP
      // Drop the HTTP header
      .dropWhile(!_.isEmpty)
      .drop(1)
      .flatMap(x =>
      {
        CreateICMP(x)
      })
  }
}
