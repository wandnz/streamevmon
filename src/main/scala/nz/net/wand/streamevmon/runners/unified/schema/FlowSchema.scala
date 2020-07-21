package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.connectors.esmond.{EsmondConnectionForeground, EsmondStreamDiscovery}
import nz.net.wand.streamevmon.detectors.HasFlinkConfig
import nz.net.wand.streamevmon.flink.{AmpMeasurementSourceFunction, BigDataSourceFunction, PollingEsmondSourceFunction}
import nz.net.wand.streamevmon.measurements.Measurement

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.functions.source.SourceFunction

import scala.collection.JavaConverters._

case class FlowSchema(
  sources: Iterable[SourceInstance],
  detectors: Map[String, DetectorSchema],
  sinks    : Iterable[SinkInstance]
)

case class SourceInstance(
  name: String,
  @JsonProperty("type")
  @JsonScalaEnumeration(classOf[SourceTypeReference])
  sourceType: SourceType.ValueBuilder,
  @JsonProperty("subtype")
  @JsonScalaEnumeration(classOf[SourceSubtypeReference])
  sourceSubtype: Option[SourceSubtype.ValueBuilder],
  config: Map[String, String]
) {
  def build: SourceFunction[Measurement] with HasFlinkConfig = {
    sourceType
      .build(sourceSubtype)
      .overrideConfig(ParameterTool.fromMap(config.asJava))
  }
}

case class SinkInstance(
  name: String,
  @JsonProperty("type")
  sinkType: String,
  config  : Map[String, String]
)

case class DetectorSchema(
  @JsonProperty("type")
  @JsonScalaEnumeration(classOf[DetectorTypeReference])
  detType: DetectorType.Value,
  config : Map[String, String],
  instances: Iterable[DetectorInstance]
)

case class DetectorInstance(
  @JsonProperty("source")
  sources: Iterable[SourceReference],
  @JsonProperty("sink")
  sinks  : Iterable[SinkReference]
)

case class SourceReference(
  name: String,
  datatype: String,
  filterLossy: Boolean
)

case class SinkReference(
  name: String
)

object SourceType extends Enumeration {

  class ValueBuilder(name: String) extends Val(name) {
    def build(
      subtype        : Option[SourceSubtype.ValueBuilder]
    ): SourceFunction[Measurement] with HasFlinkConfig = {
      this match {
        case Influx => subtype match {
          case Some(value) => value match {
            case SourceSubtype.Amp |
                 SourceSubtype.Bigdata =>
              value.build()
            case _ => throw new IllegalArgumentException(s"Cannot build $this type source with $value subtype!")
          }
          case None => throw new IllegalArgumentException(s"Cannot build $this type source with no subtype!")
        }
        case Esmond =>
          println("Esmond!")
          new PollingEsmondSourceFunction[
            EsmondConnectionForeground,
            EsmondStreamDiscovery[EsmondConnectionForeground]
          ]()
            .asInstanceOf[SourceFunction[Measurement] with HasFlinkConfig]
      }
    }
  }

  val Influx: ValueBuilder = new ValueBuilder("influx")
  val Esmond: ValueBuilder = new ValueBuilder("esmond")
}

object SourceSubtype extends Enumeration {

  class ValueBuilder(name: String) extends Val(name) {
    def build(): SourceFunction[Measurement] with HasFlinkConfig = {
      this match {
        case Amp =>
          println("Influx amp!")
          new AmpMeasurementSourceFunction().asInstanceOf[SourceFunction[Measurement] with HasFlinkConfig]
        case Bigdata =>
          println("Influx esmond!")
          new BigDataSourceFunction().asInstanceOf[SourceFunction[Measurement] with HasFlinkConfig]
      }
    }
  }

  val Amp: ValueBuilder = new ValueBuilder("amp")
  val Bigdata: ValueBuilder = new ValueBuilder("bigdata")
}

object DetectorType extends Enumeration {
  val Baseline: DetectorType.Value = Value("baseline")
  val Loss: DetectorType.Value = Value("loss")
}

class SourceTypeReference extends TypeReference[SourceType.type]

class SourceSubtypeReference extends TypeReference[SourceSubtype.type]

class DetectorTypeReference extends TypeReference[DetectorType.type]
