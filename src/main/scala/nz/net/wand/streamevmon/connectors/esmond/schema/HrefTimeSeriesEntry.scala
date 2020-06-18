package nz.net.wand.streamevmon.connectors.esmond.schema

import com.fasterxml.jackson.annotation.{JsonProperty, JsonPropertyOrder}

@JsonPropertyOrder(alphabetic = true)
class HrefTimeSeriesEntry extends AbstractTimeSeriesEntry {
  @JsonProperty("val")
  val value: Map[String, String] = Map()

  lazy val hrefLocation: Option[String] = value.get("href")

  override def toString: String = {
    s"$timestamp: ${value.values}"
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[HrefTimeSeriesEntry]

  override def equals(other: Any): Boolean = other match {
    case that: HrefTimeSeriesEntry =>
      (that canEqual this) &&
        value == that.value &&
        hrefLocation == that.hrefLocation
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(value, hrefLocation)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
