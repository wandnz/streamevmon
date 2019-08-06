package nz.net.wand.amp.analyser.measurements

import java.time.{Instant, ZoneId}

case class RichTraceroute(
    stream: Int,
    source: String,
    destination: String,
    family: String,
    packet_size_selection: String,
    path_length: Double,
    time: Instant
) extends RichMeasurement {

  override def toString: String = {
    s"${Traceroute.table_name}," +
      s"stream=$stream " +
      s"source=$source," +
      s"destination=$destination," +
      s"family=$family," +
      s"packet_size_selection=$packet_size_selection," +
      s"path_length=$path_length " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }
}

object RichTraceroute extends RichMeasurementFactory {

  override def create(base: Measurement, meta: MeasurementMeta): Option[RichTraceroute] = {
    base match {
      case b: Traceroute =>
        meta match {
          case m: TracerouteMeta =>
            Some(
              RichTraceroute(
                m.stream,
                m.source,
                m.destination,
                m.family,
                m.packet_size_selection,
                b.path_length,
                b.time
              ))
          case _ => None
        }
      case _ => None
    }
  }
}
