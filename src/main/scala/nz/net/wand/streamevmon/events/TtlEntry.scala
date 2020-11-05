package nz.net.wand.streamevmon.events

case class TtlEntry(
  source: Host,
  destination: Host,
  ttl: Double
)
