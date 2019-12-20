package nz.net.wand.streamevmon.connectors

import nz.net.wand.streamevmon.measurements._
import nz.net.wand.streamevmon.measurements.bigdata.Flow

import java.net.InetAddress
import java.time.Instant

import com.github.fsanaulla.chronicler.core.alias.ErrorOr
import com.github.fsanaulla.chronicler.core.model.InfluxReader
import org.typelevel.jawn.ast.{JArray, JValue}

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

      try {
        Right(
          ICMP(
            js.get(cols.indexOf("stream")).asString.toInt,
            js.get(cols.indexOf("loss")).asInt,
            js.get(cols.indexOf("lossrate")).asDouble,
            nullToOption(js.get(cols.indexOf("median"))).map(_.asInt),
            js.get(cols.indexOf("packet_size")).asInt,
            js.get(cols.indexOf("results")).asInt,
            s"'${js.get(cols.indexOf("rtts")).asString}'",
            Instant.parse(js.get(cols.indexOf("time")).asString)
          ))
      } catch {
        case e: Exception => Left(e)
      }
    }

    override def readUnsafe(js: JArray): ICMP = ???
  }

  val dnsReader: InfluxReader[DNS] = new InfluxReader[DNS] {
    override def read(js: JArray): ErrorOr[DNS] = {
      val cols = DNS.columnNames

      try {
        Right(
          DNS(
            js.get(cols.indexOf("stream")).asString.toInt,
            nullToOption(js.get(cols.indexOf("flag_aa"))).map(_.asBoolean),
            nullToOption(js.get(cols.indexOf("flag_ad"))).map(_.asBoolean),
            nullToOption(js.get(cols.indexOf("flag_cd"))).map(_.asBoolean),
            nullToOption(js.get(cols.indexOf("flag_qr"))).map(_.asBoolean),
            nullToOption(js.get(cols.indexOf("flag_ra"))).map(_.asBoolean),
            nullToOption(js.get(cols.indexOf("flag_rd"))).map(_.asBoolean),
            nullToOption(js.get(cols.indexOf("flag_tc"))).map(_.asBoolean),
            js.get(cols.indexOf("lossrate")).asDouble,
            nullToOption(js.get(cols.indexOf("opcode"))).map(_.asInt),
            js.get(cols.indexOf("query_len")).asInt,
            nullToOption(js.get(cols.indexOf("rcode"))).map(_.asInt),
            js.get(cols.indexOf("requests")).asInt,
            nullToOption(js.get(cols.indexOf("response_size"))).map(_.asInt),
            nullToOption(js.get(cols.indexOf("rtt"))).map(_.asInt),
            nullToOption(js.get(cols.indexOf("total_additional"))).map(_.asInt),
            nullToOption(js.get(cols.indexOf("total_answer"))).map(_.asInt),
            nullToOption(js.get(cols.indexOf("total_authority"))).map(_.asInt),
            nullToOption(js.get(cols.indexOf("ttl"))).map(_.asInt),
            Instant.parse(js.get(cols.indexOf("time")).asString)
          ))
      } catch {
        case e: Exception => Left(e)
      }
    }

    override def readUnsafe(js: JArray): DNS = ???
  }

  val httpReader: InfluxReader[HTTP] = new InfluxReader[HTTP] {
    override def read(js: JArray): ErrorOr[HTTP] = {
      val cols = HTTP.columnNames

      try {
        Right(
          HTTP(
            js.get(cols.indexOf("stream")).asString.toInt,
            js.get(cols.indexOf("bytes")).asInt,
            js.get(cols.indexOf("duration")).asInt,
            js.get(cols.indexOf("object_count")).asInt,
            js.get(cols.indexOf("server_count")).asInt,
            Instant.parse(js.get(cols.indexOf("time")).asString)
          ))
      } catch {
        case e: Exception => Left(e)
      }
    }

    override def readUnsafe(js: JArray): HTTP = ???
  }

  val tcppingReader: InfluxReader[TCPPing] = new InfluxReader[TCPPing] {
    override def read(js: JArray): ErrorOr[TCPPing] = {
      val cols = TCPPing.columnNames

      try {
        Right(
          TCPPing(
            js.get(cols.indexOf("stream")).asString.toInt,
            js.get(cols.indexOf("icmperrors")).asInt,
            js.get(cols.indexOf("loss")).asInt,
            js.get(cols.indexOf("lossrate")).asDouble, {
              val median = js.get(cols.indexOf("median")).asInt
              if (median == -1) {
                None
              }
              else {
                Some(median)
              }
            },
            js.get(cols.indexOf("packet_size")).asInt,
            js.get(cols.indexOf("results")).asInt,
            s"'${js.get(cols.indexOf("rtts")).asString}'",
            Instant.parse(js.get(cols.indexOf("time")).asString)
          ))
      } catch {
        case e: Exception => Left(e)
      }
    }

    override def readUnsafe(js: JArray): TCPPing = ???
  }

  val tracerouteReader: InfluxReader[Traceroute] = new InfluxReader[Traceroute] {
    override def read(js: JArray): ErrorOr[Traceroute] = {
      val cols = Traceroute.columnNames

      try {
        Right(
          Traceroute(
            js.get(cols.indexOf("stream")).asString.toInt,
            js.get(cols.indexOf("path_length")).asInt,
            Instant.parse(js.get(cols.indexOf("time")).asString)
          ))
      }
      catch {
        case e: Exception => Left(e)
      }
    }

    override def readUnsafe(js: JArray): Traceroute = ???
  }

  val flowStatisticsReader: InfluxReader[Flow] = new InfluxReader[Flow] {
    override def read(js: JArray): ErrorOr[Flow] = {
      val cols = Flow.columnNames

      try {
        Right(
          Flow(
            js.get(cols.indexOf("capture_application")).asString,
            js.get(cols.indexOf("capture_host")).asString,
            js.get(cols.indexOf("stream")).asInt,
            js.get(cols.indexOf("flow_type")).asString,
            js.get(cols.indexOf("category")).asString,
            js.get(cols.indexOf("protocol")).asString,
            Instant.parse(js.get(cols.indexOf("time")).asString),
            Instant.parse(js.get(cols.indexOf("start_time")).asString),
            Option(Instant.parse(js.get(cols.indexOf("end_time")).asString)),
            js.get(cols.indexOf("duration")).asDouble,
            js.get(cols.indexOf("in_bytes")).asInt,
            js.get(cols.indexOf("out_bytes")).asInt,
            js.get(cols.indexOf("time_to_first_byte")).asDouble,
            InetAddress.getByName(js.get(cols.indexOf("destination_ip")).asString),
            js.get(cols.indexOf("destination_port")).asInt,
            js.get(cols.indexOf("destination_ip_city")).asString,
            js.get(cols.indexOf("destination_ip_country")).asString,
            js.get(cols.indexOf("destination_ip_geohash")).asString,
            js.get(cols.indexOf("destination_ip_geohash_value")).asString,
            js.get(cols.indexOf("destination_ip_latitude")).asString,
            js.get(cols.indexOf("destination_ip_longitude")).asString,
            InetAddress.getByName(js.get(cols.indexOf("source_ip")).asString),
            js.get(cols.indexOf("source_port")).asInt,
            js.get(cols.indexOf("source_ip_city")).asString,
            js.get(cols.indexOf("source_ip_country")).asString,
            js.get(cols.indexOf("source_ip_geohash")).asString,
            js.get(cols.indexOf("source_ip_geohash_value")).asString,
            js.get(cols.indexOf("source_ip_latitude")).asString,
            js.get(cols.indexOf("source_ip_longitude")).asString,
          )
        )
      }
      catch {
        case e: Exception => Left(e)
      }
    }

    override def readUnsafe(js: JArray): Flow = ???
  }
}
