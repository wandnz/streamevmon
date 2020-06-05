package nz.net.wand.streamevmon.connectors.esmond.schema

import nz.net.wand.streamevmon.connectors.esmond.EsmondAPI

import java.io.Serializable

import com.fasterxml.jackson.annotation.{JsonProperty, JsonPropertyOrder}

/** A metadata archive's event-type list should contain some of these.
  *
  * @see [[EsmondAPI.archive]]
  */
@JsonPropertyOrder(alphabetic = true)
class EventType extends Serializable {

  @JsonProperty("base-uri")
  var baseUri: String = _

  @JsonProperty("event-type")
  var eventType: String = _

  @JsonProperty("summaries")
  var summaries: List[Summary] = List[Summary]()

  @JsonProperty("time-updated")
  var timeUpdated: Int = _
}
