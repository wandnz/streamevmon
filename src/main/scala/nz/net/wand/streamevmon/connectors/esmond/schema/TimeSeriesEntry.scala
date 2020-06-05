package nz.net.wand.streamevmon.connectors.esmond.schema

import nz.net.wand.streamevmon.connectors.esmond.EsmondAPI

import com.fasterxml.jackson.annotation.{JsonProperty, JsonPropertyOrder}

/** @see [[EsmondAPI.timeSeriesBase]]
  * @see [[EsmondAPI.timeSeriesSummary]]
  */
@JsonPropertyOrder(alphabetic = true)
class TimeSeriesEntry extends Serializable {
  @JsonProperty("ts")
  var timestamp: Int = _

  @JsonProperty("val")
  var value: Double = _

  override def toString: String = {
    s"$timestamp: $value"
  }
}
