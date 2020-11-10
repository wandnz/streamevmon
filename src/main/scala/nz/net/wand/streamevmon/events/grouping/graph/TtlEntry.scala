package nz.net.wand.streamevmon.events.grouping.graph

case class TtlEntry(
  source: Host,
  destination: Host,
  ttl: Double
)
