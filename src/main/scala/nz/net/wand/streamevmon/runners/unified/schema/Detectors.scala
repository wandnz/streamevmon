package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.detectors.HasFlinkConfig
import nz.net.wand.streamevmon.detectors.baseline.BaselineDetector
import nz.net.wand.streamevmon.detectors.changepoint.{ChangepointDetector, NormalDistribution}
import nz.net.wand.streamevmon.detectors.loss.LossDetector
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.detectors.spike.SpikeDetector
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.{CsvOutputable, HasDefault, Measurement}
import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.bigdata.Flow
import nz.net.wand.streamevmon.measurements.esmond._
import nz.net.wand.streamevmon.runners.unified.UnifiedYamlRunner.Perhaps

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._

import scala.reflect._

case class DetectorSchema(
  @JsonProperty("type")
  @JsonScalaEnumeration(classOf[DetectorTypeReference])
  detType: DetectorType.ValueBuilder,
  instances: Iterable[DetectorInstance]
) {
  def build: Iterable[(DetectorInstance, KeyedProcessFunction[String, Measurement, Event] with HasFlinkConfig)] = {
    instances.map(inst => (inst, inst.build(detType)))
  }
}

case class DetectorInstance(
  @JsonProperty("source")
  sources: Iterable[SourceReference],
  @JsonProperty("sink")
  sinks: Iterable[SinkReference],
  config : Map[String, String] = Map()
) {
  def build(
    detType: DetectorType.ValueBuilder
  ): KeyedProcessFunction[String, Measurement, Event] with HasFlinkConfig = {
    // Currently, all detectors have a single input type
    val source = sources.headOption.getOrElse(
      throw new IllegalArgumentException("Detector instance must have at least one source!")
    )
    val det = source.datatype match {
      case SourceReferenceDatatype.DNS => detType.build[DNS]
      case SourceReferenceDatatype.HTTP => detType.build[HTTP]
      case SourceReferenceDatatype.ICMP => detType.build[ICMP]
      case SourceReferenceDatatype.TCPPing => detType.build[TCPPing]
      case SourceReferenceDatatype.Traceroute => detType.build[Traceroute]
      case SourceReferenceDatatype.Flow => detType.build[Flow]
      case SourceReferenceDatatype.Failure => detType.build[Failure]
      case SourceReferenceDatatype.Histogram => detType.build[Histogram]
      case SourceReferenceDatatype.Href => detType.build[Href]
      case SourceReferenceDatatype.PacketTrace => detType.build[PacketTrace]
      case SourceReferenceDatatype.Simple => detType.build[Simple]
      case SourceReferenceDatatype.Subinterval => detType.build[Subinterval]
    }
    det.overrideConfig(config, s"detector.${det.configKeyGroup}")
  }
}

object DetectorType extends Enumeration {

  class ValueBuilder(name: String) extends Val(name) {
    def build[MeasT <: Measurement : ClassTag](
      implicit hasDefault: Perhaps[MeasT <:< HasDefault],
      csvOutputable      : Perhaps[MeasT <:< CsvOutputable]
    ): KeyedProcessFunction[String, Measurement, Event] with HasFlinkConfig = {
      lazy val noHasDefaultException = new IllegalArgumentException(s"Could not create $this detector as ${classTag[MeasT].toString()} does not have HasDefault!")
      lazy val noCsvOutputableException = new IllegalArgumentException(s"Could not create $this detector as ${classTag[MeasT].toString()} does not have CsvOutputable!")
      val detector = this match {
        case Baseline =>
          if (hasDefault.isDefined) {
            new BaselineDetector[MeasT with HasDefault]
          }
          else {
            throw noHasDefaultException
          }
        case Changepoint =>
          if (hasDefault.isDefined) {
            implicit val normalDistributionTypeInformation: TypeInformation[NormalDistribution[MeasT with HasDefault]] =
              TypeInformation.of(classOf[NormalDistribution[MeasT with HasDefault]])
            new ChangepointDetector[MeasT with HasDefault, NormalDistribution[MeasT with HasDefault]](
              new NormalDistribution(mean = 0)
            )
          }
          else {
            throw noHasDefaultException
          }
        case DistDiff =>
          if (hasDefault.isDefined) {
            new BaselineDetector[MeasT with HasDefault]
          }
          else {
            throw noHasDefaultException
          }
        case Loss => new LossDetector[MeasT]
        case Mode =>
          if (hasDefault.isDefined) {
            new ModeDetector[MeasT with HasDefault]
          }
          else {
            throw noHasDefaultException
          }
        case Spike =>
          if (hasDefault.isDefined) {
            new SpikeDetector[MeasT with HasDefault]
          }
          else {
            throw noHasDefaultException
          }
      }
      detector.asInstanceOf[KeyedProcessFunction[String, Measurement, Event] with HasFlinkConfig]
    }
  }

  val Baseline: ValueBuilder = new ValueBuilder("baseline")
  val Changepoint: ValueBuilder = new ValueBuilder("changepoint")
  val DistDiff: ValueBuilder = new ValueBuilder("distdiff")
  val Loss: ValueBuilder = new ValueBuilder("loss")
  val Mode: ValueBuilder = new ValueBuilder("mode")
  val Spike: ValueBuilder = new ValueBuilder("spike")
}

class DetectorTypeReference extends TypeReference[DetectorType.type]
