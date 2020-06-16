package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.measurements._

import java.time.{Instant, ZoneId}

/** Represents an AMP HTTP measurement, as well as the metadata associated with
  * the scheduled test that generated it.
  *
  * @see [[HTTP]]
  * @see [[HTTPMeta]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-http]]
  */
case class RichHTTP(
  stream                               : Int,
  source                               : String,
  destination                          : String,
  max_connections                      : Int,
  max_connections_per_server           : Int,
  max_persistent_connections_per_server: Int,
  pipelining_max_requests              : Int,
  persist                              : Boolean,
  pipelining                           : Boolean,
  caching                              : Boolean,
  bytes                                : Int,
  duration                             : Int,
  object_count                         : Int,
  server_count                         : Int,
  time                                 : Instant
) extends RichInfluxMeasurement {
  override def toString: String = {
    s"${HTTP.table_name}," +
      s"stream=$stream " +
      s"source=$source," +
      s"destination=$destination," +
      s"max_connections=$max_connections," +
      s"max_connections_per_server=$max_connections_per_server," +
      s"max_persistent_connections_per_server=$max_persistent_connections_per_server," +
      s"pipelining_max_requests=$pipelining_max_requests," +
      s"persist=$persist," +
      s"pipelining=$pipelining," +
      s"caching=$caching," +
      s"bytes=$bytes," +
      s"duration=$duration," +
      s"object_count=$object_count," +
      s"server_count=$server_count " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def isLossy: Boolean = false

  override def toCsvFormat: Seq[String] = RichHTTP.unapply(this).get.productIterator.toSeq.map(toCsvEntry)

  var defaultValue: Option[Double] = Some(bytes)
}

object RichHTTP extends RichInfluxMeasurementFactory {
  override def create(base: Measurement, meta: MeasurementMeta): Option[RichHTTP] = {
    base match {
      case b: HTTP =>
        meta match {
          case m: HTTPMeta =>
            Some(
              RichHTTP(
                m.stream,
                m.source,
                m.destination,
                m.max_connections,
                m.max_connections_per_server,
                m.max_persistent_connections_per_server,
                m.pipelining_max_requests,
                m.persist,
                m.pipelining,
                m.caching,
                b.bytes,
                b.duration,
                b.object_count,
                b.server_count,
                b.time
              ))
          case _ => None
        }
      case _ => None
    }
  }
}
