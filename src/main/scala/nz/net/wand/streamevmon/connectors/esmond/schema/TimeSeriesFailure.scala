package nz.net.wand.streamevmon.connectors.esmond.schema

import com.fasterxml.jackson.annotation.{JsonProperty, JsonPropertyOrder}

@JsonPropertyOrder(alphabetic = true)
class TimeSeriesFailure extends AbstractTimeSeriesEntry {
  @JsonProperty("val")
  val value: Map[String, String] = Map()

  val failureText: Option[String] = value.get("error")

  override def toString: String = {
    val item = value.headOption.getOrElse(throw new IllegalStateException("Serialisation error!"))
    s"${item._1}: ${item._2}"
  }
}
