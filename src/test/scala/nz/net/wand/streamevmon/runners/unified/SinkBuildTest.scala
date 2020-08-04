package nz.net.wand.streamevmon.runners.unified

import nz.net.wand.streamevmon.TestBase
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.sinks.InfluxSinkFunction
import nz.net.wand.streamevmon.runners.unified.schema.{SinkInstance, SinkType}

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction

import scala.collection.JavaConverters._

class SinkBuildTest extends TestBase {
  "Sources should build correctly" when {
    "built by a SinkInstance" when {
      "sink type is Influx" in {
        val srcInstance = SinkInstance(
          SinkType.Influx,
          config = Map("extraKey" -> "true")
        )
        val built = srcInstance.build

        built shouldBe an[InfluxSinkFunction]
        built.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"sink.${built.configKeyGroup}.extraKey" -> "true")
      }

      "sink type is Print" in {
        val srcInstance = SinkInstance(
          SinkType.Print,
          config = Map("extraKey" -> "true")
        )
        val built = srcInstance.build

        built shouldBe a[PrintSinkFunction[Event]]
        built.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"sink.${built.configKeyGroup}.extraKey" -> "true")
      }
    }
  }
}
