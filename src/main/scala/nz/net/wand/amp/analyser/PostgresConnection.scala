package nz.net.wand.amp.analyser

import nz.net.wand.amp.analyser.measurements._

import com.typesafe.config.{Config, ConfigFactory}
import io.getquill.{PostgresJdbcContext, SnakeCase}

object PostgresConnection extends Logging with Configuration with Caching {

  @transient final private[analyser] lazy val ctx =
    new PostgresJdbcContext(SnakeCase, getConfigRoot)

  def getConfigRoot: Config = {
    val postgresConfigRoot =
      getConfigString("postgres.configRoot").getOrElse(s"$configPrefix.postgres")
    ConfigFactory.load().getConfig(postgresConfigRoot)
  }
  import ctx._

  def getICMPMeta(base: ICMP): Option[ICMPMeta] =
    getWithCache(
      s"icmp.${base.stream}", {
        val query = quote(unquote(schema.icmp).filter(t => t.stream == lift(base.stream.toInt)))
        ctx.run(query).headOption
      }
    )

  def getDNSMeta(base: DNS): Option[DNSMeta] =
    getWithCache(
      s"dns.${base.stream}", {
        val query = quote(unquote(schema.dns).filter(t => t.stream == lift(base.stream.toInt)))
        ctx.run(query).headOption
      }
    )

  def getTracerouteMeta(base: Traceroute): Option[TracerouteMeta] =
    getWithCache(
      s"traceroute.${base.stream}", {
        val query =
          quote(unquote(schema.traceroute).filter(t => t.stream == lift(base.stream.toInt)))
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

  private[analyser] object schema {
    final lazy val icmp: Quoted[EntityQuery[ICMPMeta]] = quote {
      querySchema[ICMPMeta]("streams_amp_icmp", _.stream -> "stream_id")
    }

    final lazy val dns: Quoted[EntityQuery[DNSMeta]] = quote {
      querySchema[DNSMeta]("streams_amp_dns", _.stream -> "stream_id")
    }

    final lazy val traceroute: Quoted[EntityQuery[TracerouteMeta]] = quote {
      querySchema[TracerouteMeta]("streams_amp_traceroute", _.stream -> "stream_id")
    }
  }
}
