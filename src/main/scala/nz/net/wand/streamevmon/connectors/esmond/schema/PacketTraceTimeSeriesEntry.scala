package nz.net.wand.streamevmon.connectors.esmond.schema

import com.fasterxml.jackson.annotation.{JsonProperty, JsonPropertyOrder}

@JsonPropertyOrder(alphabetic = true)
class PacketTraceTimeSeriesEntry extends AbstractTimeSeriesEntry {
  @JsonProperty("val")
  val value: Iterable[PacketTraceEntry] = Seq()
}

@JsonPropertyOrder(alphabetic = true)
class ASEntry extends Serializable {
  @JsonProperty("owner")
  val owner: String = null
  @JsonProperty("number")
  val number: Int = Int.MinValue
}

@JsonPropertyOrder(alphabetic = true)
class PacketTraceEntry extends Serializable {
  @JsonProperty("success")
  val success: Int = Int.MinValue
  @JsonProperty("ip")
  val ip: String = null
  @JsonProperty("hostname")
  val hostname: String = null
  @JsonProperty("rtt")
  val rtt: Double = Double.NaN
  @JsonProperty("as")
  val as: ASEntry = null
  @JsonProperty("ttl")
  val ttl: Int = Int.MinValue
  @JsonProperty("query")
  val query: Int = Int.MinValue
}
