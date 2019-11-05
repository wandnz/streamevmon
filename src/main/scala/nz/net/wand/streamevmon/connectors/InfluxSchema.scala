package nz.net.wand.streamevmon.connectors

import nz.net.wand.streamevmon.measurements._

import java.time.Instant

import com.github.fsanaulla.chronicler.core.alias.ErrorOr
import com.github.fsanaulla.chronicler.core.model.InfluxReader
import org.typelevel.jawn.ast.JArray

object InfluxSchema {

  val icmpReader: InfluxReader[ICMP] = new InfluxReader[ICMP] {
    override def read(js: JArray): ErrorOr[ICMP] = {
      val cols = ICMP.columnNames

      try {
        Right(
          ICMP(
            js.get(cols.indexOf("stream")).asString.toInt,
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

    override def readUnsafe(js: JArray): ICMP = ???
  }

  val dnsReader: InfluxReader[DNS] = new InfluxReader[DNS] {
    override def read(js: JArray): ErrorOr[DNS] = {
      val cols = DNS.columnNames

      try {
        Right(
          DNS(
            js.get(cols.indexOf("stream")).asString.toInt,
            Some(js.get(cols.indexOf("flag_aa")).asBoolean),
            Some(js.get(cols.indexOf("flag_ad")).asBoolean),
            Some(js.get(cols.indexOf("flag_cd")).asBoolean),
            Some(js.get(cols.indexOf("flag_qr")).asBoolean),
            Some(js.get(cols.indexOf("flag_ra")).asBoolean),
            Some(js.get(cols.indexOf("flag_rd")).asBoolean),
            Some(js.get(cols.indexOf("flag_tc")).asBoolean),
            js.get(cols.indexOf("lossrate")).asDouble,
            Some(js.get(cols.indexOf("opcode")).asInt),
            js.get(cols.indexOf("query_len")).asInt,
            Some(js.get(cols.indexOf("rcode")).asInt),
            js.get(cols.indexOf("requests")).asInt,
            Some(js.get(cols.indexOf("response_size")).asInt),
            Some(js.get(cols.indexOf("rtt")).asInt),
            Some(js.get(cols.indexOf("total_additional")).asInt),
            Some(js.get(cols.indexOf("total_answer")).asInt),
            Some(js.get(cols.indexOf("total_authority")).asInt),
            Some(js.get(cols.indexOf("ttl")).asInt),
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
      } catch {
        case e: Exception => Left(e)
      }
    }

    override def readUnsafe(js: JArray): Traceroute = ???
  }
}
