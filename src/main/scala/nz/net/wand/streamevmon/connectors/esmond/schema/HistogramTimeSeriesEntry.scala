package nz.net.wand.streamevmon.connectors.esmond.schema

import com.fasterxml.jackson.annotation.{JsonProperty, JsonPropertyOrder}

@JsonPropertyOrder(alphabetic = true)
class HistogramTimeSeriesEntry extends AbstractTimeSeriesEntry {

  @JsonProperty("val")
  private val valueInternal: Map[String, Int] = Map()
  // We need this shim, because Retrofit/Jackson isn't smart enough to correctly
  // cast the type at runtime, which results in what looks like a Double, but
  // is actually a String. This causes ClassCastExceptions when it tries to
  // access the value as a Double.
  lazy val value: Map[Double, Int] = valueInternal.map(v => (v._1.toDouble, v._2))

  override def toString: String = s"histogram at time ${timestamp.toString}"
}
