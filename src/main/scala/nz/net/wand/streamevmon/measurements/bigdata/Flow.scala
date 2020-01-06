package nz.net.wand.streamevmon.measurements.bigdata

import nz.net.wand.streamevmon.measurements.{Measurement, MeasurementFactory}
import nz.net.wand.streamevmon.measurements.bigdata.Flow.Endpoint
import nz.net.wand.streamevmon.measurements.bigdata.Flow.FlowType.FlowType

import java.net.InetAddress
import java.time.Instant

import org.squeryl.annotations.Column

case class Flow(
  capture_application         : String,
  capture_host: String,

  @Column("flow_id")
  stream: Int,
  @Column("type")
  flow_type                   : FlowType,
  category: String,
  protocol                    : String,

  time                        : Instant, // time of current update
  @Column("start_ts")
  start_time                  : Instant, // time of flow start
  @Column("end_ts")
  end_time                    : Option[Instant], // time of flow end,
  duration: Double,

  in_bytes                    : Int,
  out_bytes                   : Int,
  @Column("ttfb")
  time_to_first_byte          : Double,

  destination_ip: InetAddress,
  @Column("dst_port")
  destination_port            : Int,
  destination_ip_city: Option[String],
  destination_ip_country      : Option[String],
  destination_ip_geohash      : Option[String],
  destination_ip_geohash_value: Option[Int],
  destination_ip_latitude     : Option[Double],
  destination_ip_longitude    : Option[Double],

  source_ip: InetAddress,
  @Column("src_port")
  source_port                 : Int,
  source_ip_city              : Option[String],
  source_ip_country           : Option[String],
  source_ip_geohash           : Option[String],
  source_ip_geohash_value     : Option[Int],
  source_ip_latitude          : Option[Double],
  source_ip_longitude         : Option[Double],
) extends Measurement {
  override def isLossy: Boolean = false

  val includesGeolocation: Boolean = source_ip_geohash.isDefined

  /** We'd put the source and destination Endpoint objects directly in the
    * constructor, but that would hide the database column names and make it a
    * little less easy to read from it. Instead, we'll just provide these in
    * case anyone wants to use them. We could make those fields private, but
    * that of course messes with our reflection and that's even more effort to
    * fix!
    */
  lazy val source: Endpoint = Endpoint(
    source_ip,
    source_port,
    source_ip_city,
    source_ip_country,
    source_ip_geohash,
    source_ip_geohash_value,
    source_ip_latitude,
    source_ip_longitude
  )

  lazy val destination: Endpoint = Endpoint(
    destination_ip,
    destination_port,
    destination_ip_city,
    destination_ip_country,
    destination_ip_geohash,
    destination_ip_geohash_value,
    destination_ip_latitude,
    destination_ip_longitude
  )
}

object Flow extends MeasurementFactory {

  object FlowType extends Enumeration {
    type FlowType = Value
    val Start: Flow.FlowType.Value = Value("flow_start")
    val Interval: Flow.FlowType.Value = Value("flow_interval")
    val End: Flow.FlowType.Value = Value("flow_end")
  }

  /** A descriptor of the source or destination of a flow. By default, equality
    * is defined to ignore the geolocation attributes, since they are optional.
    * An ip:port combination is the same endpoint regardless of whether the
    * bigdata daemon was running with geolocation enabled.
    */
  case class Endpoint(
    ip           : InetAddress,
    port         : Int,
    city         : Option[String],
    country      : Option[String],
    geohash      : Option[String],
    geohash_value: Option[Int],
    latitude     : Option[Double],
    longitude    : Option[Double]
  ) {
    val includesGeolocation: Boolean = geohash.isDefined

    override def equals(o: Any): Boolean = o match {
      case o: Endpoint => (ip, port) == (o.ip, o.port)
      case _ => false
    }

    override def hashCode(): Int = (ip, port).##
  }

  override val table_name: String = "flow_statistics"

  override def columnNames: Seq[String] = getColumnNames[Flow]

  override private[measurements] def create(subscriptionLine: String): Option[Flow] = ???
}
