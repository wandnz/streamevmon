package nz.net.wand.streamevmon.connectors.esmond.schema

import com.fasterxml.jackson.annotation.{JsonProperty, JsonPropertyOrder}

@JsonPropertyOrder(alphabetic = true)
abstract class AbstractTimeSeriesEntry extends Serializable {
  @JsonProperty("ts")
  val timestamp: Long = Long.MinValue
}
