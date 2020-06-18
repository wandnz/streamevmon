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

  def canEqual(other: Any): Boolean = other.isInstanceOf[SimpleTimeSeriesEntry]

  override def equals(other: Any): Boolean = other match {
    case that: SimpleTimeSeriesEntry =>
      (that canEqual this) &&
        value == that.value
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(value)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
