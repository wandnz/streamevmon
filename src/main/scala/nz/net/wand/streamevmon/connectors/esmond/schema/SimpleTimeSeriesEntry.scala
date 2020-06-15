package nz.net.wand.streamevmon.connectors.esmond.schema

import nz.net.wand.streamevmon.connectors.esmond.EsmondAPI

import com.fasterxml.jackson.annotation.{JsonProperty, JsonPropertyOrder}

/** @see [[EsmondAPI.simpleTimeSeries]]
  */
@JsonPropertyOrder(alphabetic = true)
class SimpleTimeSeriesEntry extends AbstractTimeSeriesEntry {
  @JsonProperty("val")
  val value: Double = Double.NaN

  override def toString: String = {
    s"$timestamp: $value"
  }
}
