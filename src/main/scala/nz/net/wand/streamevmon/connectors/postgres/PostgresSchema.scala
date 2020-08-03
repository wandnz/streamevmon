package nz.net.wand.streamevmon.connectors.postgres

import nz.net.wand.streamevmon.connectors.postgres.SquerylEntrypoint._
import nz.net.wand.streamevmon.measurements.amp._

import org.squeryl.{Schema, Table}

/** Defines the database schema of the PostgreSQL connection. Should be used in
  * conjunction with [[SquerylEntrypoint]].
  */
object PostgresSchema extends Schema {
  val icmpMeta: Table[ICMPMeta] = table("streams_amp_icmp")
  val dnsMeta: Table[DNSMeta] = table("streams_amp_dns")
  val tracerouteMeta: Table[TracerouteMeta] = table("streams_amp_traceroute")
  val tcppingMeta: Table[TCPPingMeta] = table("streams_amp_tcpping")
  val httpMeta: Table[HTTPMeta] = table("streams_amp_http")
}
