package nz.net.wand.streamevmon.measurements.bigdata

import nz.net.wand.streamevmon.measurements.{Measurement, MeasurementFactory}

import java.net.InetAddress
import java.time.Instant

import org.squeryl.annotations.Column

case class Flow(
  capture_application: String,
  capture_host                : String,

  @Column("flow_id")
  stream: Int,
  @Column("type")
  flow_type: String, // make enum
  category: String, // maybe make enum
  protocol: String, // maybe make enum

  time: Instant, // time of current update
  @Column("start_ts")
  start_time: Instant, // time of flow start
  @Column("end_ts")
  end_time: Option[Instant], // time of flow end,
  duration                    : Double, // I have no idea what this is

  in_bytes                    : Int,
  out_bytes: Int,
  @Column("ttfb")
  time_to_first_byte          : Double,

  // this should probably all be a case class by itself
  destination_ip: InetAddress,
  @Column("dst_port")
  destination_port            : Int,
  destination_ip_city         : Option[String],
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
}

object Flow extends MeasurementFactory {
  override val table_name: String = "flow_statistics"

  override def columnNames: Seq[String] = getColumnNames[Flow]

  override private[measurements] def create(subscriptionLine: String): Option[Flow] = ???
}
