package nz.net.wand.streamevmon.connectors.esmond.schema

import com.fasterxml.jackson.annotation.{JsonProperty, JsonPropertyOrder}

@JsonPropertyOrder(alphabetic = true)
class HrefTimeSeriesEntry extends AbstractTimeSeriesEntry {

  @JsonProperty("val")
  val value: Map[String, String] = Map()

  val hrefLocation: Option[String] = value.get("href")

  override def toString: String = {
    s"$timestamp: ${value.values}"
  }
}
