package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.HasFlinkConfig

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import org.apache.flink.streaming.api.functions.sink.SinkFunction

/** From-yaml representation of a sink to build.
  *
  * @param sinkType The type of sink, such as Influx or Print.
  * @param config   Any configuration overrides that should be passed to the sink.
  */
case class SinkInstance(
  @JsonProperty("type")
  @JsonScalaEnumeration(classOf[SinkTypeReference])
  sinkType: SinkType.ValueBuilder,
  config  : Map[String, String] = Map()
) {
  /** Builds the appropriate sink with configuration overrides. */
  def build: SinkFunction[Event] with HasFlinkConfig = {
    val configPrefix = s"sink.$sinkType"

    sinkType
      .build
      .overrideConfig(config, configPrefix)
  }
}
