package nz.net.wand.streamevmon.connectors.esmond.schema

import com.fasterxml.jackson.annotation.{JsonProperty, JsonPropertyOrder}

@JsonPropertyOrder(alphabetic = true)
class SubintervalTimeSeriesEntry extends AbstractTimeSeriesEntry {
  @JsonProperty("val")
  val value: Iterable[SubintervalValue] = Seq()

  override def toString: String = s"subinterval at time ${timestamp.toString}"

  def canEqual(other: Any): Boolean = other.isInstanceOf[SubintervalTimeSeriesEntry]

  override def equals(other: Any): Boolean = other match {
    case that: SubintervalTimeSeriesEntry =>
      (that canEqual this) &&
        value == that.value
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(value)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

@JsonPropertyOrder(alphabetic = true)
class SubintervalValue extends Serializable {
  @JsonProperty("duration")
  val duration: Double = Double.NaN
  @JsonProperty("start")
  val start: Double = Double.NaN
  @JsonProperty("val")
  val value: Double = Double.NaN

  def canEqual(other: Any): Boolean = other.isInstanceOf[SubintervalValue]

  override def equals(other: Any): Boolean = other match {
    case that: SubintervalValue =>
      (that canEqual this) &&
        duration == that.duration &&
        start == that.start &&
        value == that.value
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(duration, start, value)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
