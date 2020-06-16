package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.measurements._

import java.time.{Instant, ZoneId}

/** Represents an AMP TCPPing measurement, as well as the metadata associated
  * with the scheduled test that generated it.
  *
  * @see [[TCPPing]]
  * @see [[TCPPingMeta]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-tcpping]]
  */
case class RichTCPPing(
  stream               : Int,
  source               : String,
  destination          : String,
  port                 : Int,
  family               : String,
  packet_size_selection: String,
  icmperrors           : Option[Int],
  loss                 : Option[Int],
  lossrate             : Option[Double],
  median               : Option[Int],
  packet_size          : Int,
  results              : Option[Int],
  rtts                 : Seq[Option[Int]],
  time                 : Instant
) extends RichInfluxMeasurement {
  override def toString: String = {
    s"${TCPPing.table_name}," +
      s"stream=$stream " +
      s"source=$source," +
      s"destination=$destination," +
      s"port=$port," +
      s"family=$family," +
      s"packet_size_selection=$packet_size_selection," +
      s"icmperrors=$icmperrors," +
      s"loss=$loss," +
      s"lossrate=$lossrate," +
      s"median=${median.getOrElse("")}," +
      s"packet_size=$packet_size," +
      s"results=$results," +
      s"rtts=${rtts.map(x => x.getOrElse("None")).mkString("\"[", ",", "]\"")} " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def isLossy: Boolean = loss.getOrElse(100) > 0

  override def toCsvFormat: Seq[String] = RichTCPPing.unapply(this).get.productIterator.toSeq.map(toCsvEntry)

  var defaultValue: Option[Double] = median.map(_.toDouble)
}

object RichTCPPing extends RichInfluxMeasurementFactory {
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
                m.packet_size_selection,
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
