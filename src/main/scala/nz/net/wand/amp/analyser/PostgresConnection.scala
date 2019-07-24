package nz.net.wand.amp.analyser

import nz.net.wand.amp.analyser.measurements._

import io.getquill.{PostgresJdbcContext, SnakeCase}

object PostgresConnection extends Logging with Caching {

  @transient final private[this] lazy val ctx =
    new PostgresJdbcContext(SnakeCase, "nz.net.wand.amp.analyser.postgres")
  import ctx._

  final private[this] lazy val icmpTable: Quoted[EntityQuery[ICMPMeta]] = quote {
    querySchema[ICMPMeta]("streams_amp_icmp", _.stream -> "stream_id")
  }

  final private[this] lazy val dnsTable: Quoted[EntityQuery[DNSMeta]] = quote {
    querySchema[DNSMeta]("streams_amp_dns", _.stream -> "stream_id")
  }

  final private[this] lazy val tracerouteTable: Quoted[EntityQuery[TracerouteMeta]] = quote {
    querySchema[TracerouteMeta]("streams_amp_traceroute", _.stream -> "stream_id")
  }

  def getICMPMeta(base: ICMP): Option[ICMPMeta] =
    getWithCache(
      s"icmp.${base.stream}", {
        val query = quote(unquote(icmpTable).filter(t => t.stream == lift(base.stream.toInt)))
        ctx.run(query).headOption
      }
    )

  def getDNSMeta(base: DNS): Option[DNSMeta] =
    getWithCache(
      s"dns.${base.stream}", {
        val query = quote(unquote(dnsTable).filter(t => t.stream == lift(base.stream.toInt)))
        ctx.run(query).headOption
      }
    )

  def getTracerouteMeta(base: Traceroute): Option[TracerouteMeta] =
    getWithCache(
      s"traceroute.${base.stream}", {
        val query = quote(unquote(tracerouteTable).filter(t => t.stream == lift(base.stream.toInt)))
        ctx.run(query).headOption
      }
    )

  def getMeta(base: Measurement): Option[MeasurementMeta] =
    base match {
      case x: ICMP       => getICMPMeta(x)
      case x: DNS        => getDNSMeta(x)
      case x: Traceroute => getTracerouteMeta(x)
      case _             => None
    }
}
