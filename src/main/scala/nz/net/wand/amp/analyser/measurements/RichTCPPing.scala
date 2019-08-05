package nz.net.wand.amp.analyser.measurements

import java.time.{Instant, ZoneId}

case class RichTCPPing(
    stream: Int,
    source: String,
    destination: String,
    port: Int,
    family: String,
    packet_size_selection: String,
    icmperrors: Int,
    loss: Int,
    lossrate: Double,
    median: Option[Int],
    packet_size: Int,
    results: Int,
    rtts: Seq[Option[Int]],
    time: Instant
) extends RichMeasurement {
  override def toString: String = {
    s"${TCPPing.table_name}," +
      s"stream=$stream " +
      s"source=$source " +
      s"destination=$destination " +
      s"port=$port " +
      s"family=$family " +
      s"packet_size_selection=$packet_size_selection " +
      s"icmperrors=$icmperrors " +
      s"loss=$loss " +
      s"lossrate=$lossrate " +
      s"median=$median " +
      s"packet_size=$packet_size " +
      s"results=$results " +
      s"rtts=$rtts " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }
}

object RichTCPPing extends RichMeasurementFactory {
  override def create(base: Measurement, meta: MeasurementMeta): Option[RichTCPPing] = {
    base match {
      case b: TCPPing =>
        meta match {
          case m: TCPPingMeta =>
            Some(
              RichTCPPing(
                m.stream,
                m.source,
                m.destination,
                m.port,
                m.family,
                m.packet_size,
                b.icmperrors,
                b.loss,
                b.lossrate,
                b.median,
                b.packet_size,
                b.results,
                b.rtts,
                b.time
              ))
          case _ => None
        }
      case _ => None
    }
  }
}
