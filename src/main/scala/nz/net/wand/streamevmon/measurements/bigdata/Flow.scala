package nz.net.wand.streamevmon.measurements.bigdata

import nz.net.wand.streamevmon.measurements.{InfluxMeasurement, InfluxMeasurementFactory}
import nz.net.wand.streamevmon.measurements.bigdata.Flow.Endpoint
import nz.net.wand.streamevmon.measurements.bigdata.Flow.FlowType.FlowType

import java.net.InetAddress
import java.time.Instant
import java.util.concurrent.TimeUnit

import org.squeryl.annotations.Column

/** @see [[nz.net.wand.streamevmon.measurements]] for description.
  */
case class Flow(
  capture_application                                                                               : String,
  capture_host                                                                                      : String,

  @Column("flow_id")
  stream: Int,
  @Column("type")
  flow_type: FlowType,
  category: String,
  protocol: String,

  time: Instant,
  @Column("start_ts")
  start_time: Instant,
  @Column("end_ts")
  end_time                                                                                          : Option[Instant],
  duration                                                                                          : Double,

  in_bytes                                                                                          : Int,
  out_bytes                                                                                         : Int,
  @Column("ttfb")
  time_to_first_byte                                                                                : Double,

  source_ip                                                                                         : String,
  @Column("src_port")
  source_port                                                                                       : Int,
  source_ip_city                                                                                    : Option[String],
  source_ip_country                                                                                 : Option[String],
  source_ip_geohash                                                                                 : Option[String],
  source_ip_geohash_value                                                                           : Option[Int],
  source_ip_latitude                                                                                : Option[Double],
  source_ip_longitude                                                                               : Option[Double],

  destination_ip                                                                                    : String,
  @Column("dst_port")
  destination_port                                                                                  : Int,
  destination_ip_city                                                                               : Option[String],
  destination_ip_country                                                                            : Option[String],
  destination_ip_geohash                                                                            : Option[String],
  destination_ip_geohash_value                                                                      : Option[Int],
  destination_ip_latitude                                                                           : Option[Double],
  destination_ip_longitude                                                                          : Option[Double]
) extends InfluxMeasurement {
  override def isLossy: Boolean = false

  // Case classes don't generate unapply methods for more than 22 fields, since
  // the biggest Tuple is 22 items.
  override def toCsvFormat: Seq[String] = Seq(
    capture_application,
    capture_host,
    stream,
    flow_type,
    category,
    protocol,
    time,
    start_time,
    end_time,
    duration,
    in_bytes,
    out_bytes,
    time_to_first_byte,
    source_ip,
    source_port,
    source_ip_city,
    source_ip_country,
    source_ip_geohash,
    source_ip_geohash_value,
    source_ip_latitude,
    source_ip_longitude,
    destination_ip,
    destination_port,
    destination_ip_city,
    destination_ip_country,
    destination_ip_geohash,
    destination_ip_geohash_value,
    destination_ip_latitude,
    destination_ip_longitude
  ).map(toCsvEntry)

  var defaultValue: Option[Double] = Some(time_to_first_byte)

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

object Flow extends InfluxMeasurementFactory {

  /** Flow entries can be one of three types, depending on what part of a flow
    * they represent. A flow usually has a start, an end, and 0 or more intervals.
    */
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
    ip           : String,
    port         : Int,
    city         : Option[String],
    country      : Option[String],
    geohash      : Option[String],
    geohash_value: Option[Int],
    latitude     : Option[Double],
    longitude    : Option[Double]
  ) {
    val includesGeolocation: Boolean = geohash.isDefined

    lazy val address: InetAddress = InetAddress.getByName(ip)

    override def equals(o: Any): Boolean = o match {
      case o: Endpoint => (ip, port) == (o.ip, o.port)
      case _ => false
    }

    override def hashCode(): Int = (ip, port).##
  }

  override val table_name: String = "flow_statistics"

  override def columnNames: Seq[String] = getColumnNames[Flow]

  override def create(subscriptionLine: String): Option[Flow] = {
    val data = splitLineProtocol(subscriptionLine)
    if (data.head != table_name) {
      None
    }
    else {
      Some(
        Flow(
          getNamedField(data, "capture_application").get,
          getNamedField(data, "capture_host").get,
          getNamedField(data, "flow_id").get.dropRight(1).toInt,
          FlowType.withName(getNamedField(data, "type").get),
          getNamedField(data, "category").get,
          getNamedField(data, "protocol").get,
          Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(data.last.toLong)),
          Instant.ofEpochMilli(getNamedField(data, "start_ts").get.dropRight(1).toLong),
          getNamedField(data, "end_ts").map(e => Instant.ofEpochMilli(e.dropRight(1).toLong)),
          getNamedField(data, "duration").get.toDouble,
          getNamedField(data, "in_bytes").get.dropRight(1).toInt,
          getNamedField(data, "out_bytes").get.dropRight(1).toInt,
          getNamedField(data, "ttfb").get.toDouble,
          getNamedField(data, "source_ip").get.drop(1).dropRight(1),
          getNamedField(data, "src_port").get.dropRight(1).toInt,
          getNamedField(data, "source_ip_city").map(_.drop(1).dropRight(1)),
          getNamedField(data, "source_ip_country").map(_.drop(1).dropRight(1)),
          getNamedField(data, "source_ip_geohash"),
          getNamedField(data, "source_ip_geohash_value").map(_.toInt),
          getNamedField(data, "source_ip_latitude").map(_.toDouble),
          getNamedField(data, "source_ip_longitude").map(_.toDouble),
          getNamedField(data, "destination_ip").get.drop(1).dropRight(1),
          getNamedField(data, "dst_port").get.dropRight(1).toInt,
          getNamedField(data, "destination_ip_city").map(_.drop(1).dropRight(1)),
          getNamedField(data, "destination_ip_country").map(_.drop(1).dropRight(1)),
          getNamedField(data, "destination_ip_geohash"),
          getNamedField(data, "destination_ip_geohash_value").map(_.dropRight(1).toInt),
          getNamedField(data, "destination_ip_latitude").map(_.toDouble),
          getNamedField(data, "destination_ip_longitude").map(_.toDouble),
        )
      )
    }
  }
}
