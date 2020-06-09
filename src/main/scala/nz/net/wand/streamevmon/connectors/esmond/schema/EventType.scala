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
  val baseUri: String = ""

  @JsonProperty("event-type")
  val eventType: String = ""

  @JsonProperty("summaries")
  val summaries: List[Summary] = List[Summary]()

  @JsonProperty("time-updated")
  val timeUpdated: Long = Long.MinValue

  def canEqual(other: Any): Boolean = other.isInstanceOf[EventType]

  override def equals(other: Any): Boolean = other match {
    case that: EventType =>
      (that canEqual this) &&
        baseUri == that.baseUri &&
        eventType == that.eventType &&
        summaries == that.summaries &&
        timeUpdated == that.timeUpdated
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(baseUri, eventType, summaries, timeUpdated)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
