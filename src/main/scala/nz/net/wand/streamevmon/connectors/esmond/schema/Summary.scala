package nz.net.wand.streamevmon.connectors.esmond.schema

import java.io.Serializable

import com.fasterxml.jackson.annotation.{JsonProperty, JsonPropertyOrder}

@JsonPropertyOrder(alphabetic = true)
class Summary extends Serializable {

  @JsonProperty("summary-type")
  var summaryType: String = _

  @JsonProperty("summary-window")
  var summaryWindow: String = _

  @JsonProperty("time-updated")
  var timeUpdated: Int = _

  @JsonProperty("uri")
  var uri: String = _
}
