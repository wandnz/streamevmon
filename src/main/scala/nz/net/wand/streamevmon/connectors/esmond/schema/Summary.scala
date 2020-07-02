package nz.net.wand.streamevmon.connectors.esmond.schema

import nz.net.wand.streamevmon.connectors.esmond.EsmondAPI

import java.io.Serializable

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonPropertyOrder}

/** An entry in a metadata archive's event-type field might contain some of these.
  *
  * @see [[EsmondAPI.archive]]
  */
@JsonPropertyOrder(alphabetic = true)
class Summary extends Serializable {
  // Unfortunately, this field is usually different to the part of the URI that
  // it corresponds to. We'll have to expose a field which bypasses the
  // discrepancies, instead of the raw field.
  // It would be tidier and safer to use an enum for this, since there are only
  // three possibilities at the time of writing. However, it could be expanded
  // later and it's more work for not much benefit.
  @JsonProperty("summary-type")
  protected val summaryTypeRaw: String = ""
  @JsonIgnore
  lazy val summaryType: String = {
    if (Summary.summaryTypeOverrides.contains(summaryTypeRaw)) {
      Summary.summaryTypeOverrides(summaryTypeRaw)
    }
    else {
      summaryTypeRaw
    }
  }

  @JsonProperty("summary-window")
  val summaryWindow: Long = Long.MinValue

  @JsonProperty("time-updated")
  val timeUpdated: Long = Long.MinValue

  @JsonProperty("uri")
  val uri: String = ""

  // These fields could probably be obtained more elegantly, but it does work
  // for getting the fields which are otherwise missing.
  @JsonIgnore
  lazy val metadataKey: String = uri.split('/')(4)

  @JsonIgnore
  lazy val eventType: String = uri.split('/')(5)

  override def toString: String = uri

  def canEqual(other: Any): Boolean = other.isInstanceOf[Summary]

  override def equals(other: Any): Boolean = other match {
    case that: Summary =>
      (that canEqual this) &&
        summaryType == that.summaryType &&
        summaryWindow == that.summaryWindow &&
        timeUpdated == that.timeUpdated &&
        uri == that.uri
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(summaryType, summaryWindow, timeUpdated, uri)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object Summary {
  protected val summaryTypeOverrides = Map(
    "aggregation" -> "aggregations",
    "average" -> "averages"
  )
}
