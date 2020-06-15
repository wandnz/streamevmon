package nz.net.wand.streamevmon.connectors.esmond.schema

import com.fasterxml.jackson.annotation.{JsonProperty, JsonPropertyOrder}

@JsonPropertyOrder(alphabetic = true)
class SubintervalTimeSeriesEntry extends AbstractTimeSeriesEntry {

  @JsonProperty("val")
  val value: Iterable[SubintervalValue] = Seq()

  override def toString: String = s"subinterval at time ${timestamp.toString}"
}

@JsonPropertyOrder(alphabetic = true)
class SubintervalValue extends Serializable {
  @JsonProperty("duration")
  val duration: Double = Double.NaN
  @JsonProperty("start")
  val start: Double = Double.NaN
  @JsonProperty("val")
  val value: Double = Double.NaN
}
