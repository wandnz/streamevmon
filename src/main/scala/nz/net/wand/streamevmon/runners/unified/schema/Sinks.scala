package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.detectors.HasFlinkConfig
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.InfluxSinkFunction

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import org.apache.flink.streaming.api.functions.sink.{PrintSinkFunction, SinkFunction}

case class SinkInstance(
  @JsonProperty("type")
  @JsonScalaEnumeration(classOf[SinkTypeReference])
  sinkType: SinkType.ValueBuilder,
  config  : Map[String, String] = Map()
) {
  def build: SinkFunction[Event] with HasFlinkConfig = {
    val configPrefix = s"sink.$sinkType"

    sinkType
      .build
      .overrideConfig(config, configPrefix)
  }
}

case class SinkReference(
  name: String
)

object SinkType extends Enumeration {

  class ValueBuilder(name: String) extends Val(name) {
    def build: SinkFunction[Event] with HasFlinkConfig = {
      this match {
        case Influx => new InfluxSinkFunction
        case Print => new PrintSinkFunction[Event] with HasFlinkConfig {
          override val flinkName: String = "Print: Std Out"
          override val flinkUid: String = "print-sink"
          override val configKeyGroup: String = ""
        }
      }
    }
  }

  val Influx: ValueBuilder = new ValueBuilder("influx")
  val Print: ValueBuilder = new ValueBuilder("print")
}

class SinkTypeReference extends TypeReference[SinkType.type]
