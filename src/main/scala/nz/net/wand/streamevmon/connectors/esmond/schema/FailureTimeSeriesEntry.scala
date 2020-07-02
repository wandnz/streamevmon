package nz.net.wand.streamevmon.connectors.esmond.schema

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonPropertyOrder}

@JsonPropertyOrder(alphabetic = true)
class FailureTimeSeriesEntry extends AbstractTimeSeriesEntry {
  @JsonProperty("val")
  val value: Map[String, String] = Map()

  @JsonIgnore
  lazy val failureText: Option[String] = value.get("error")

  override def toString: String = {
    val item = value.headOption.getOrElse(throw new IllegalStateException("Serialisation error!"))
    s"${item._1}: ${item._2}"
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[FailureTimeSeriesEntry]

  override def equals(other: Any): Boolean = other match {
    case that: FailureTimeSeriesEntry =>
      (that canEqual this) &&
        value == that.value &&
        failureText == that.failureText
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(value, failureText)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
