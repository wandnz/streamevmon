package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.connectors.esmond.{EsmondConnectionForeground, EsmondStreamDiscovery}
import nz.net.wand.streamevmon.detectors.HasFlinkConfig
import nz.net.wand.streamevmon.flink.{AmpMeasurementSourceFunction, BigDataSourceFunction, PollingEsmondSourceFunction}
import nz.net.wand.streamevmon.measurements.Measurement

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import org.apache.flink.streaming.api.functions.source.SourceFunction

case class SourceInstance(
  @JsonProperty("type")
  @JsonScalaEnumeration(classOf[SourceTypeReference])
  sourceType: SourceType.ValueBuilder,
  @JsonProperty("subtype")
  @JsonScalaEnumeration(classOf[SourceSubtypeReference])
  sourceSubtype: Option[SourceSubtype.ValueBuilder],
  config: Map[String, String] = Map()
) {
  def build: SourceFunction[Measurement] with HasFlinkConfig = {
    val configPrefixNoSubtype = s"source.$sourceType"
    val configPrefix = sourceSubtype.map(s => s"$configPrefixNoSubtype.$s")
      .getOrElse(configPrefixNoSubtype)

    sourceType
      .build(sourceSubtype)
      .overrideConfig(config, configPrefix)
  }
}

case class SourceReference(
  name : String,
  @JsonScalaEnumeration(classOf[SourceReferenceDatatypeReference])
  datatype: SourceReferenceDatatype.Value,
  filterLossy: Boolean
)

object SourceType extends Enumeration {

  class ValueBuilder(name: String) extends Val(name) {
    def build(
      subtype: Option[SourceSubtype.ValueBuilder]
    ): SourceFunction[Measurement] with HasFlinkConfig = {
      this match {
        case Influx => subtype match {
          case Some(value) => value match {
            case SourceSubtype.Amp |
                 SourceSubtype.Bigdata =>
              value.build()
            case _ => throw new IllegalArgumentException(s"Cannot build $this type source with ${subtype.getOrElse("no")} subtype!")
          }
          case None => throw new IllegalArgumentException(s"Cannot build $this type source with no subtype!")
        }
        case Esmond =>
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
        case Amp => new AmpMeasurementSourceFunction().asInstanceOf[SourceFunction[Measurement] with HasFlinkConfig]
        case Bigdata => new BigDataSourceFunction().asInstanceOf[SourceFunction[Measurement] with HasFlinkConfig]
      }
    }
  }

  val Amp: ValueBuilder = new ValueBuilder("amp")
  val Bigdata: ValueBuilder = new ValueBuilder("bigdata")
}

object SourceReferenceDatatype extends Enumeration {
  val DNS: Value = Value("dns")
  val HTTP: Value = Value("http")
  val ICMP: Value = Value("icmp")
  val TCPPing: Value = Value("tcpping")
  val Traceroute: Value = Value("traceroute")
  val Flow: Value = Value("flow")
}

class SourceTypeReference extends TypeReference[SourceType.type]

class SourceSubtypeReference extends TypeReference[SourceSubtype.type]

class SourceReferenceDatatypeReference extends TypeReference[SourceReferenceDatatype.type]
