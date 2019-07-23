package nz.net.wand.amp.analyser
import nz.net.wand.amp.analyser.measurements._

import io.getquill.{PostgresJdbcContext, SnakeCase}

object PostgresConnection extends Logging {
  lazy val ctx = new PostgresJdbcContext(SnakeCase, "nz.net.wand.amp.analyser")
  import ctx._

  val traceroutes: Quoted[EntityQuery[TracerouteMeta]] = quote {
    querySchema[TracerouteMeta]("streams_amp_traceroute", _.stream -> "stream_id")
  }

  val icmps: Quoted[EntityQuery[ICMPMeta]] = quote {
    querySchema[ICMPMeta]("streams_amp_icmp", _.stream -> "stream_id")
  }

  def getTracerouteMeta(base: Traceroute): Option[TracerouteMeta] = {
    val query = quote(unquote(traceroutes).filter(t => t.stream == lift(base.stream.toInt)))

    logger.debug(s"Running PostgreSQL query: ${ctx.translate(query)}")

    ctx.run(query).headOption
  }

  def getICMPMeta(base: ICMP): Option[ICMPMeta] = {
    val query = quote(unquote(icmps).filter(t => t.stream == lift(base.stream.toInt)))

    logger.debug(s"Running PostgreSQL query: ${ctx.translate(query)}")

    ctx.run(query).headOption
  }

  def getMeta(base: Measurement): Option[MeasurementMeta] =
    base match {
      case x: Traceroute => getTracerouteMeta(x)
      case x: ICMP       => getICMPMeta(x)
      case _             => None
    }
}
