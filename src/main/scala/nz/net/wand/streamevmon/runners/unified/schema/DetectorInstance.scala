package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.detectors.HasFlinkConfig
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.bigdata.Flow
import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.esmond._

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.flink.streaming.api.functions.KeyedProcessFunction

/** Represents the configuration of a detector. Detector type is informed by
  * the DetectorSchema which holds this class. Measurement type is informed by
  * the sources involved.
  *
  * @param sources References the source's user-defined name, the datatype to
  *                get from it, and whether or not to filter lossy measurements
  *                out.
  * @param sinks   References each sink that should be used with this detector.
  * @param config  Any configuration overrides to be passed to the detector.
  */
case class DetectorInstance(
  @JsonProperty("source")
  sources: Iterable[SourceReference],
  @JsonProperty("sink")
  sinks: Iterable[SinkReference],
  config : Map[String, String] = Map()
) {

  import nz.net.wand.streamevmon.Perhaps._

  /** Builds a detector instance with the appropriate measurement type. */
  def build(
    detType: DetectorType.ValueBuilder
  ): KeyedProcessFunction[String, Measurement, Event] with HasFlinkConfig = {
    // Currently, all detectors have a single input type.
    val source = sources.headOption.getOrElse(
      throw new IllegalArgumentException("Detector instance must have at least one source!")
    )
    // The detector type knows how to build itself, but it needs to know what
    // type its measurements will be.
    val det = source.datatype match {
      case SourceDatatype.DNS => detType.build[DNS]
      case SourceDatatype.HTTP => detType.build[HTTP]
      case SourceDatatype.ICMP => detType.build[ICMP]
      case SourceDatatype.TCPPing => detType.build[TCPPing]
      case SourceDatatype.Traceroute => detType.build[Traceroute]
      case SourceDatatype.Flow => detType.build[Flow]
      case SourceDatatype.Failure => detType.build[Failure]
      case SourceDatatype.Histogram => detType.build[Histogram]
      case SourceDatatype.Href => detType.build[Href]
      case SourceDatatype.PacketTrace => detType.build[PacketTrace]
      case SourceDatatype.Simple => detType.build[Simple]
      case SourceDatatype.Subinterval => detType.build[Subinterval]
    }
    det.overrideConfig(config, s"detector.${det.configKeyGroup}")
  }
}
