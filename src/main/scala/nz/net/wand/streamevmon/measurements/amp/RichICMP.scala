package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.measurements._

import java.time.{Instant, ZoneId}

/** Represents an AMP ICMP measurement, as well as the metadata associated with
  * the scheduled test that generated it.
  *
  * @see [[ICMP]]
  * @see [[ICMPMeta]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-icmp]]
  */
case class RichICMP(
  stream               : Int,
  source               : String,
  destination          : String,
  family               : String,
  packet_size_selection: String,
  loss                 : Option[Int],
  lossrate             : Option[Double],
  median               : Option[Int],
  packet_size          : Int,
  results              : Option[Int],
  rtts                 : Seq[Option[Int]],
  time                 : Instant
) extends RichInfluxMeasurement {
  override def toString: String = {
    s"${ICMP.table_name}," +
      s"stream=$stream " +
      s"source=$source," +
      s"destination=$destination," +
      s"family=$family," +
      s"packet_size_selection=$packet_size_selection " +
      s"loss=$loss," +
      s"lossrate=$lossrate," +
      s"median=${median.getOrElse("")}," +
      s"packet_size=$packet_size," +
      s"results=$results," +
      s"rtts=${rtts.map(x => x.getOrElse("None")).mkString("\"[", ",", "]\"")} " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def isLossy: Boolean = loss.getOrElse(100) > 0

  override def toCsvFormat: Seq[String] = RichICMP.unapply(this).get.productIterator.toSeq.map(toCsvEntry)

  var defaultValue: Option[Double] = median.map(_.toDouble)
}

object RichICMP extends RichInfluxMeasurementFactory {

  override def create(base: Measurement, meta: MeasurementMeta): Option[RichICMP] = {
    base match {
      case b: ICMP =>
        meta match {
          case m: ICMPMeta =>
            Some(
              RichICMP(
                m.stream,
                m.source,
                m.destination,
                m.family,
                m.packet_size_selection,
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
