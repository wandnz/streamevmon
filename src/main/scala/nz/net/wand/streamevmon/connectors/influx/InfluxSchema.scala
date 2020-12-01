package nz.net.wand.streamevmon.connectors.influx

import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.bigdata.Flow
import nz.net.wand.streamevmon.measurements.bigdata.Flow.FlowType

import java.time.Instant

import com.github.fsanaulla.chronicler.core.alias.ErrorOr
import com.github.fsanaulla.chronicler.core.model.InfluxReader
import org.typelevel.jawn.ast.{JArray, JValue}

import scala.util.Try

/** Declares the Reader objects for measurements obtained directly from InfluxDB
  * via Chronicler by [[InfluxHistoryConnection]]. These are used to convert the
  * JArray objects into Measurement objects, and unfortunately involve a lot of
  * hard-coding. The Measurement.columnNames function allows us to ensure that
  * the results are ordered consistently, no matter whether the tables get
  * sorted by Influx differently. Chronicler notes that Influx sorts columns
  * alphabetically, but doing it this way lets us write slightly nicer code.
  *
  * We don't bother implementing the readUnsafe functions, since they haven't
  * been observed to be called.
  */
object InfluxSchema {

  def nullToOption(v: JValue): Option[JValue] = {
    if (v.isNull) {
      None
    }
    else {
      Some(v)
    }
  }

  val icmpReader: InfluxReader[ICMP] = new InfluxReader[ICMP] {
    override def read(js: JArray): ErrorOr[ICMP] = {
      val cols = ICMP.columnNames

      Try(
        ICMP(
          js.get(cols.indexOf("stream")).asString,
          nullToOption(js.get(cols.indexOf("loss"))).map(_.asInt),
          nullToOption(js.get(cols.indexOf("lossrate"))).map(_.asDouble),
          nullToOption(js.get(cols.indexOf("median"))).map(_.asInt),
          js.get(cols.indexOf("packet_size")).asInt,
          nullToOption(js.get(cols.indexOf("results"))).map(_.asInt),
          s"'${js.get(cols.indexOf("rtts")).asString}'",
          Instant.parse(js.get(cols.indexOf("time")).asString)
        )
      ).toEither
    }

    override def readUnsafe(js: JArray): ICMP = ???
  }

  val dnsReader: InfluxReader[DNS] = new InfluxReader[DNS] {
    override def read(js: JArray): ErrorOr[DNS] = {
      val cols = DNS.columnNames

      Try(
        DNS(
          js.get(cols.indexOf("stream")).asString,
          nullToOption(js.get(cols.indexOf("flag_aa"))).map(_.asBoolean),
          nullToOption(js.get(cols.indexOf("flag_ad"))).map(_.asBoolean),
          nullToOption(js.get(cols.indexOf("flag_cd"))).map(_.asBoolean),
          nullToOption(js.get(cols.indexOf("flag_qr"))).map(_.asBoolean),
          nullToOption(js.get(cols.indexOf("flag_ra"))).map(_.asBoolean),
          nullToOption(js.get(cols.indexOf("flag_rd"))).map(_.asBoolean),
          nullToOption(js.get(cols.indexOf("flag_tc"))).map(_.asBoolean),
          nullToOption(js.get(cols.indexOf("lossrate"))).map(_.asDouble),
          nullToOption(js.get(cols.indexOf("opcode"))).map(_.asInt),
          nullToOption(js.get(cols.indexOf("query_len"))).map(_.asInt),
          nullToOption(js.get(cols.indexOf("rcode"))).map(_.asInt),
          js.get(cols.indexOf("requests")).asInt,
          nullToOption(js.get(cols.indexOf("response_size"))).map(_.asInt),
          nullToOption(js.get(cols.indexOf("rtt"))).map(_.asInt),
          nullToOption(js.get(cols.indexOf("total_additional"))).map(_.asInt),
          nullToOption(js.get(cols.indexOf("total_answer"))).map(_.asInt),
          nullToOption(js.get(cols.indexOf("total_authority"))).map(_.asInt),
          nullToOption(js.get(cols.indexOf("ttl"))).map(_.asInt),
          Instant.parse(js.get(cols.indexOf("time")).asString)
        )
      ).toEither
    }

    override def readUnsafe(js: JArray): DNS = ???
  }

  val httpReader: InfluxReader[HTTP] = new InfluxReader[HTTP] {
    override def read(js: JArray): ErrorOr[HTTP] = {
      val cols = HTTP.columnNames

      val r = Try(
        HTTP(
          js.get(cols.indexOf("stream")).asString,
          nullToOption(js.get(cols.indexOf("bytes"))).map(_.asInt),
          nullToOption(js.get(cols.indexOf("duration"))).map(_.asInt),
          js.get(cols.indexOf("object_count")).asInt,
          js.get(cols.indexOf("server_count")).asInt,
          Instant.parse(js.get(cols.indexOf("time")).asString)
        )
      ).toEither
      if (r.isLeft) {
        val b = 1
      }
      r
    }

    override def readUnsafe(js: JArray): HTTP = ???
  }

  val tcppingReader: InfluxReader[TCPPing] = new InfluxReader[TCPPing] {
    override def read(js: JArray): ErrorOr[TCPPing] = {
      val cols = TCPPing.columnNames

      Try(
        TCPPing(
          js.get(cols.indexOf("stream")).asString,
          nullToOption(js.get(cols.indexOf("icmperrors"))).map(_.asInt),
          nullToOption(js.get(cols.indexOf("loss"))).map(_.asInt),
          nullToOption(js.get(cols.indexOf("lossrate"))).map(_.asDouble),
          nullToOption(js.get(cols.indexOf("median"))).map(_.asInt),
          js.get(cols.indexOf("packet_size")).asInt,
          nullToOption(js.get(cols.indexOf("results"))).map(_.asInt),
          s"'${js.get(cols.indexOf("rtts")).asString}'",
          Instant.parse(js.get(cols.indexOf("time")).asString)
        )
      ).toEither
    }

    override def readUnsafe(js: JArray): TCPPing = ???
  }

  val traceroutePathlenReader: InfluxReader[TraceroutePathlen] = new InfluxReader[TraceroutePathlen] {
    override def read(js: JArray): ErrorOr[TraceroutePathlen] = {
      val cols = TraceroutePathlen.columnNames

      Try(
        TraceroutePathlen(
          js.get(cols.indexOf("stream")).asString,
          nullToOption(js.get(cols.indexOf("path_length"))).map(_.asInt),
          Instant.parse(js.get(cols.indexOf("time")).asString)
        )
      ).toEither
    }

    override def readUnsafe(js: JArray): TraceroutePathlen = ???
  }

  val flowStatisticsReader: InfluxReader[Flow] = new InfluxReader[Flow] {
    override def read(js: JArray): ErrorOr[Flow] = {
      val cols = Flow.columnNames

      Try(
        Flow(
          js.get(cols.indexOf("capture_application")).asString,
          js.get(cols.indexOf("capture_host")).asString,
          js.get(cols.indexOf("flow_id")).asInt.toString,
          FlowType.withName(js.get(cols.indexOf("type")).asString),
          js.get(cols.indexOf("category")).asString,
          js.get(cols.indexOf("protocol")).asString,
          Instant.parse(js.get(cols.indexOf("time")).asString),
          Instant.ofEpochMilli(js.get(cols.indexOf("start_ts")).asLong),
          nullToOption(js.get(cols.indexOf("end_ts"))).map(e => Instant.ofEpochMilli(e.asLong)),
          js.get(cols.indexOf("duration")).asDouble,
          js.get(cols.indexOf("in_bytes")).asInt,
          js.get(cols.indexOf("out_bytes")).asInt,
          js.get(cols.indexOf("ttfb")).asDouble,
          js.get(cols.indexOf("source_ip")).asString,
          js.get(cols.indexOf("src_port")).asInt,
          nullToOption(js.get(cols.indexOf("source_ip_city"))).map(_.asString),
          nullToOption(js.get(cols.indexOf("source_ip_country"))).map(_.asString),
          nullToOption(js.get(cols.indexOf("source_ip_geohash"))).map(_.asString),
          nullToOption(js.get(cols.indexOf("source_ip_geohash_value"))).map(_.asInt),
          nullToOption(js.get(cols.indexOf("source_ip_latitude"))).map(_.asDouble),
          nullToOption(js.get(cols.indexOf("source_ip_longitude"))).map(_.asDouble),
          js.get(cols.indexOf("destination_ip")).asString,
          js.get(cols.indexOf("dst_port")).asInt,
          nullToOption(js.get(cols.indexOf("destination_ip_city"))).map(_.asString),
          nullToOption(js.get(cols.indexOf("destination_ip_country"))).map(_.asString),
          nullToOption(js.get(cols.indexOf("destination_ip_geohash"))).map(_.asString),
          nullToOption(js.get(cols.indexOf("destination_ip_geohash_value"))).map(_.asInt),
          nullToOption(js.get(cols.indexOf("destination_ip_latitude"))).map(_.asDouble),
          nullToOption(js.get(cols.indexOf("destination_ip_longitude"))).map(_.asDouble)
        )
      ).toEither
    }

    override def readUnsafe(js: JArray): Flow = ???
  }
}
