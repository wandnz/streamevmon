package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.measurements._

import java.time.{Instant, ZoneId}

/** Represents an AMP Traceroute measurement, as well as the metadata associated
  * with the scheduled test that generated it.
  *
  * @see [[Traceroute]]
  * @see [[TracerouteMeta]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-trace]]
  */
case class RichTraceroute(
  stream               : Int,
  source               : String,
  destination          : String,
  family               : String,
  packet_size_selection: String,
  path_length          : Option[Double],
  time                 : Instant
) extends RichInfluxMeasurement {

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

  override def isLossy: Boolean = false

  override def toCsvFormat: Seq[String] = RichTraceroute.unapply(this).get.productIterator.toSeq.map(toCsvEntry)

  var defaultValue: Option[Double] = path_length
}

object RichTraceroute extends RichInfluxMeasurementFactory {

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
