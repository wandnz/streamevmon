package nz.net.wand.amp.analyser.measurements

import nz.net.wand.amp.analyser.SquerylEntrypoint._

import org.squeryl.{Schema, Table}

object PostgresSchema extends Schema {
  val icmpMeta: Table[ICMPMeta] = table("streams_amp_icmp")
  val dnsMeta: Table[DNSMeta] = table("streams_amp_dns")
  val tracerouteMeta: Table[TracerouteMeta] = table("streams_amp_traceroute")
}
