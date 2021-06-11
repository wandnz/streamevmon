/* This file is part of streamevmon.
 *
 * Copyright (C) 2021  The University of Waikato, Hamilton, New Zealand
 *
 * Author: Daniel Oosterwijk
 *
 * All rights reserved.
 *
 * This code has been developed by the University of Waikato WAND
 * research group. For further information please see https://wand.nz,
 * or our Github organisation at https://github.com/wanduow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.net.wand.streamevmon.connectors.influx

import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.amp2.{Traceroute => A2Traceroute, _}
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

  object amp2 {

    import scala.reflect.runtime.universe.TypeTag

    abstract class Amp2ReaderParent[T <: Amp2Measurement : TypeTag] extends InfluxReader[T] {
      override def readUnsafe(js: JArray): T = ???

      override def read(js: JArray): ErrorOr[T] = {
        Try(interpretJArray(Amp2Measurement.getColumnNames[T], js)).toEither
      }

      def interpretJArray(cols: Seq[String], js: JArray): T
    }

    val external: InfluxReader[External] = new Amp2ReaderParent[External] {
      override def interpretJArray(cols: Seq[String], js: JArray): External =
        External(
          js.get(cols.indexOf("source")).asString,
          js.get(cols.indexOf("destination")).asString,
          js.get(cols.indexOf("test")).asString,
          Instant.parse(js.get(cols.indexOf("time")).asString),
          js.get(cols.indexOf("command")).asString,
          nullToOption(js.get(cols.indexOf("count"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("value"))).map(_.asLong),
        )
    }

    val fastping: InfluxReader[Fastping] = new Amp2ReaderParent[Fastping] {
      override def interpretJArray(cols: Seq[String], js: JArray): Fastping =
        Fastping(
          js.get(cols.indexOf("source")).asString,
          js.get(cols.indexOf("destination")).asString,
          js.get(cols.indexOf("test")).asString,
          Instant.parse(js.get(cols.indexOf("time")).asString),
          js.get(cols.indexOf("dscp")).asString,
          js.get(cols.indexOf("family")).asString,
          js.get(cols.indexOf("packet_count")).asString.toLong,
          js.get(cols.indexOf("packet_rate")).asString.toLong,
          js.get(cols.indexOf("packet_size")).asString.toLong,
          nullToOption(js.get(cols.indexOf("count"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("jitter"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("loss_percent"))).map(_.asDouble),
          nullToOption(js.get(cols.indexOf("rtt"))).map(_.asLong),
        )
    }
    val http: InfluxReader[Http] = new Amp2ReaderParent[Http] {
      override def interpretJArray(cols: Seq[String], js: JArray): Http =
        Http(
          js.get(cols.indexOf("source")).asString,
          js.get(cols.indexOf("destination")).asString,
          js.get(cols.indexOf("test")).asString,
          Instant.parse(js.get(cols.indexOf("time")).asString),
          js.get(cols.indexOf("caching")).asString,
          nullToOption(js.get(cols.indexOf("bytes"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("count"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("duration"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("object_count"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("server_count"))).map(_.asLong),
        )
    }
    val latency: InfluxReader[Latency] = new Amp2ReaderParent[Latency] {
      override def interpretJArray(cols: Seq[String], js: JArray): Latency = {
        js.get(cols.indexOf("test")).asString match {
          case "dns" => LatencyDns(
            js.get(cols.indexOf("source")).asString,
            js.get(cols.indexOf("destination")).asString,
            js.get(cols.indexOf("test")).asString,
            Instant.parse(js.get(cols.indexOf("time")).asString),
            js.get(cols.indexOf("dscp")).asString,
            js.get(cols.indexOf("family")).asString,
            js.get(cols.indexOf("query")).asString,
            nullToOption(js.get(cols.indexOf("count"))).map(_.asLong),
            nullToOption(js.get(cols.indexOf("error_code"))).map(_.asLong),
            nullToOption(js.get(cols.indexOf("error_type"))).map(_.asLong),
            nullToOption(js.get(cols.indexOf("icmpcode"))).map(_.asLong),
            nullToOption(js.get(cols.indexOf("icmptype"))).map(_.asLong),
            nullToOption(js.get(cols.indexOf("loss"))).map(_.asLong),
            nullToOption(js.get(cols.indexOf("rtt"))).map(_.asLong),
          )
          case "icmp" => LatencyIcmp(
            js.get(cols.indexOf("source")).asString,
            js.get(cols.indexOf("destination")).asString,
            js.get(cols.indexOf("test")).asString,
            Instant.parse(js.get(cols.indexOf("time")).asString),
            js.get(cols.indexOf("dscp")).asString,
            js.get(cols.indexOf("family")).asString,
            js.get(cols.indexOf("packet_size")).asString.toLong,
            js.get(cols.indexOf("random")).asString.toBoolean,
            nullToOption(js.get(cols.indexOf("count"))).map(_.asLong),
            nullToOption(js.get(cols.indexOf("error_code"))).map(_.asLong),
            nullToOption(js.get(cols.indexOf("error_type"))).map(_.asLong),
            nullToOption(js.get(cols.indexOf("icmpcode"))).map(_.asLong),
            nullToOption(js.get(cols.indexOf("icmptype"))).map(_.asLong),
            nullToOption(js.get(cols.indexOf("loss"))).map(_.asLong),
            nullToOption(js.get(cols.indexOf("rtt"))).map(_.asLong),
          )
          case "tcpping" => LatencyTcpping(
            js.get(cols.indexOf("source")).asString,
            js.get(cols.indexOf("destination")).asString,
            js.get(cols.indexOf("test")).asString,
            Instant.parse(js.get(cols.indexOf("time")).asString),
            js.get(cols.indexOf("dscp")).asString,
            js.get(cols.indexOf("family")).asString,
            js.get(cols.indexOf("packet_size")).asString.toLong,
            js.get(cols.indexOf("port")).asString.toLong,
            js.get(cols.indexOf("random")).asString.toBoolean,
            nullToOption(js.get(cols.indexOf("count"))).map(_.asLong),
            nullToOption(js.get(cols.indexOf("error_code"))).map(_.asLong),
            nullToOption(js.get(cols.indexOf("error_type"))).map(_.asLong),
            nullToOption(js.get(cols.indexOf("icmpcode"))).map(_.asLong),
            nullToOption(js.get(cols.indexOf("icmptype"))).map(_.asLong),
            nullToOption(js.get(cols.indexOf("loss"))).map(_.asLong),
            nullToOption(js.get(cols.indexOf("rtt"))).map(_.asLong),
          )
        }
      }

    }
    val pathlen: InfluxReader[Pathlen] = new Amp2ReaderParent[Pathlen] {
      override def interpretJArray(cols: Seq[String], js: JArray): Pathlen =
        Pathlen(
          js.get(cols.indexOf("source")).asString,
          js.get(cols.indexOf("destination")).asString,
          js.get(cols.indexOf("test")).asString,
          Instant.parse(js.get(cols.indexOf("time")).asString),
          js.get(cols.indexOf("dscp")).asString,
          js.get(cols.indexOf("family")).asString,
          js.get(cols.indexOf("packet_size")).asString.toLong,
          js.get(cols.indexOf("random")).asString.toBoolean,
          nullToOption(js.get(cols.indexOf("length"))).map(_.asLong),
        )
    }
    val sip: InfluxReader[Sip] = new Amp2ReaderParent[Sip] {
      override def interpretJArray(cols: Seq[String], js: JArray): Sip =
        Sip(
          js.get(cols.indexOf("source")).asString,
          js.get(cols.indexOf("destination")).asString,
          js.get(cols.indexOf("test")).asString,
          Instant.parse(js.get(cols.indexOf("time")).asString),
          js.get(cols.indexOf("dscp")).asString,
          js.get(cols.indexOf("family")).asString,
          js.get(cols.indexOf("uri")).asString,
          nullToOption(js.get(cols.indexOf("connect_time"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("count"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("duration"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("response_time"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("rtt"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("rx_jitter"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("rx_loss_percent"))).map(_.asDouble),
          nullToOption(js.get(cols.indexOf("tx_jitter"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("tx_loss_percent"))).map(_.asDouble),
        )
    }
    val throughput: InfluxReader[Throughput] = new Amp2ReaderParent[Throughput] {
      override def interpretJArray(cols: Seq[String], js: JArray): Throughput =
        Throughput(
          js.get(cols.indexOf("source")).asString,
          js.get(cols.indexOf("destination")).asString,
          js.get(cols.indexOf("test")).asString,
          Instant.parse(js.get(cols.indexOf("time")).asString),
          Direction(js.get(cols.indexOf("direction")).asString),
          js.get(cols.indexOf("dscp")).asString,
          js.get(cols.indexOf("family")).asString,
          js.get(cols.indexOf("protocol")).asString,
          js.get(cols.indexOf("write_size")).asString.toLong,
          nullToOption(js.get(cols.indexOf("bytes"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("count"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("duration"))).map(_.asString),
          nullToOption(js.get(cols.indexOf("runtime"))).map(_.asDouble),
        )
    }
    val traceroute: InfluxReader[A2Traceroute] = new Amp2ReaderParent[A2Traceroute] {
      override def interpretJArray(cols: Seq[String], js: JArray): A2Traceroute =
        A2Traceroute(
          js.get(cols.indexOf("source")).asString,
          js.get(cols.indexOf("destination")).asString,
          js.get(cols.indexOf("test")).asString,
          Instant.parse(js.get(cols.indexOf("time")).asString),
          js.get(cols.indexOf("dscp")).asString,
          js.get(cols.indexOf("family")).asString,
          js.get(cols.indexOf("hop")).asString.toLong,
          js.get(cols.indexOf("packet_size")).asString.toLong,
          js.get(cols.indexOf("random")).asString.toBoolean,
          nullToOption(js.get(cols.indexOf("address"))).map(_.asString),
          nullToOption(js.get(cols.indexOf("rtt"))).map(_.asLong),
        )
    }
    val udpstream: InfluxReader[Udpstream] = new Amp2ReaderParent[Udpstream] {
      override def interpretJArray(cols: Seq[String], js: JArray): Udpstream =
        Udpstream(
          js.get(cols.indexOf("source")).asString,
          js.get(cols.indexOf("destination")).asString,
          js.get(cols.indexOf("test")).asString,
          Instant.parse(js.get(cols.indexOf("time")).asString),
          Direction(js.get(cols.indexOf("direction")).asString),
          js.get(cols.indexOf("dscp")).asString,
          js.get(cols.indexOf("family")).asString,
          js.get(cols.indexOf("packet_count")).asString.toLong,
          js.get(cols.indexOf("packet_size")).asString.toLong,
          js.get(cols.indexOf("packet_spacing")).asString.toLong,
          nullToOption(js.get(cols.indexOf("count"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("jitter"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("loss"))).map(_.asDouble),
          nullToOption(js.get(cols.indexOf("mos"))).map(_.asDouble),
          nullToOption(js.get(cols.indexOf("rtt"))).map(_.asLong),
        )
    }

    val video: InfluxReader[Video] = new Amp2ReaderParent[Video] {
      override def interpretJArray(cols: Seq[String], js: JArray): Video =
        Video(
          js.get(cols.indexOf("source")).asString,
          js.get(cols.indexOf("destination")).asString,
          js.get(cols.indexOf("test")).asString,
          Instant.parse(js.get(cols.indexOf("time")).asString),
          js.get(cols.indexOf("requested_quality")).asString,
          nullToOption(js.get(cols.indexOf("actual_quality"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("count"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("initial_buffering"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("playing_time"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("pre_time"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("stall_count"))).map(_.asLong),
          nullToOption(js.get(cols.indexOf("stall_time"))).map(_.asLong),
        )
    }
  }

  object amp {
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
  }

  object bigdata {
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
}
