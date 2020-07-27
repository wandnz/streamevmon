package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.detectors.HasFlinkConfig
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.bigdata.Flow
import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.esmond._

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.windows.Window

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
  def buildKeyed(
    detType: DetectorType.ValueBuilder
  ): KeyedProcessFunction[String, Measurement, Event] with HasFlinkConfig = {
    // Currently, all detectors have a single input type.
    val source = sources.headOption.getOrElse(
      throw new IllegalArgumentException("Detector instance must have at least one source!")
    )
    // The detector type knows how to build itself, but it needs to know what
    // type its measurements will be.
    val det = source.datatype match {
      case SourceDatatype.DNS => detType.buildKeyed[DNS]
      case SourceDatatype.HTTP => detType.buildKeyed[HTTP]
      case SourceDatatype.ICMP => detType.buildKeyed[ICMP]
      case SourceDatatype.TCPPing => detType.buildKeyed[TCPPing]
      case SourceDatatype.Traceroute => detType.buildKeyed[Traceroute]
      case SourceDatatype.Flow => detType.buildKeyed[Flow]
      case SourceDatatype.Failure => detType.buildKeyed[Failure]
      case SourceDatatype.Histogram => detType.buildKeyed[Histogram]
      case SourceDatatype.Href => detType.buildKeyed[Href]
      case SourceDatatype.PacketTrace => detType.buildKeyed[PacketTrace]
      case SourceDatatype.Simple => detType.buildKeyed[Simple]
      case SourceDatatype.Subinterval => detType.buildKeyed[Subinterval]
    }

    det.overrideConfig(config, s"detector.${det.configKeyGroup}")
  }

  def buildWindowed(
    detType: DetectorType.ValueBuilder
  ): (ProcessWindowFunction[Measurement, Event, String, Window] with HasFlinkConfig, StreamWindowType.Value) = {
    val source = sources.headOption.getOrElse(
      throw new IllegalArgumentException("Detector instance must have at least one source!")
    )

    val det = source.datatype match {
      case SourceDatatype.DNS => detType.buildWindowed[DNS]
      case SourceDatatype.HTTP => detType.buildWindowed[HTTP]
      case SourceDatatype.ICMP => detType.buildWindowed[ICMP]
      case SourceDatatype.TCPPing => detType.buildWindowed[TCPPing]
      case SourceDatatype.Traceroute => detType.buildWindowed[Traceroute]
      case SourceDatatype.Flow => detType.buildWindowed[Flow]
      case SourceDatatype.Failure => detType.buildWindowed[Failure]
      case SourceDatatype.Histogram => detType.buildWindowed[Histogram]
      case SourceDatatype.Href => detType.buildWindowed[Href]
      case SourceDatatype.PacketTrace => detType.buildWindowed[PacketTrace]
      case SourceDatatype.Simple => detType.buildWindowed[Simple]
      case SourceDatatype.Subinterval => detType.buildWindowed[Subinterval]
    }

    (
      det._1.overrideConfig(config, s"detector.${det._1.configKeyGroup}"),
      det._2
    )
  }
}