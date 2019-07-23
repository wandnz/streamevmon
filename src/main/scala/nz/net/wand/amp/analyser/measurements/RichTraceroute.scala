package nz.net.wand.amp.analyser.measurements

case class RichTraceroute(
    source: String,
    destination: String,
    family: String,
    packet_size: String,
    path_length: Double,
    time: Long
) extends RichMeasurement {

  override def toString: String = {
    s"${Traceroute.table_name} " +
      s"source=$source," +
      s"destination=$destination," +
      s"family=$family," +
      s"packet_size=$packet_size," +
      s"path_length=$path_length " +
      s"$time"
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
                m.source,
                m.destination,
                m.family,
                m.packet_size,
                b.path_length,
                b.time
              ))
          case _ => None
        }
      case _ => None
    }
  }
}
