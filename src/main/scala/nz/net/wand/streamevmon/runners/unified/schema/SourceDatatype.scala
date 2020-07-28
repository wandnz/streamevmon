package nz.net.wand.streamevmon.runners.unified.schema

/** This lets Jackson map strings in the YAML file to an enum we can use in pattern matching. */
object SourceDatatype extends Enumeration {
  // AMP
  val DNS: Value = Value("dns")
  val HTTP: Value = Value("http")
  val ICMP: Value = Value("icmp")
  val TCPPing: Value = Value("tcpping")
  val Traceroute: Value = Value("traceroute")
  // Bigdata
  val Flow: Value = Value("flow")
  // Esmond
  val Failure: Value = Value("failure")
  val Histogram: Value = Value("histogram")
  val Href: Value = Value("href")
  val PacketTrace: Value = Value("packettrace")
  val Simple: Value = Value("simple")
  val Subinterval: Value = Value("subinterval")
  // Latency TS
  val LatencyTSAmp: Value = Value("ampicmp")
  val LatencyTSSmokeping: Value = Value("latencytssmokeping")
}
