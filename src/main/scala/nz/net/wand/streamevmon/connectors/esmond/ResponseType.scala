package nz.net.wand.streamevmon.connectors.esmond

import nz.net.wand.streamevmon.Logging

/** Translates the event type string from an API response to an enum value
  * corresponding to a schema.TimeSeriesEntry type. Mainly used by
  * [[EsmondConnectionForeground]], which includes a function to map these
  * types to constructors.
  */
object ResponseType extends Enumeration with Logging {
  type ResponseType = Value
  val Failure: Value = Value
  val Histogram: Value = Value
  val Href: Value = Value
  val PacketTrace: Value = Value
  val Simple: Value = Value
  val Subintervals: Value = Value

  def fromString(eventType: String): ResponseType = eventType.toLowerCase match {
    case "failures" => Failure
    case "histogram-ttl" | "histogram-owdelay" => Histogram
    case "pscheduler-run-href" => Href
    case "packet-trace" => PacketTrace
    case "time-error-estimates" |
         "packet-duplicates" |
         "packet-loss-rate" |
         "packet-count-sent" |
         "packet-count-lost" |
         "throughput" |
         "packet-retransmits" |
         "packet-reorders"
    => Simple
    case "throughput-subintervals" | "packet-retransmits-subintervals" => Subintervals
    case "path-mtu" => logger.error("Found path-mtu data!"); Simple
    case other: String => throw new IllegalArgumentException(s"Unknown response type $other")
  }
}
