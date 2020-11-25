package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.traits.Measurement

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.windows.Window

/** This is the highest level of representation for detectors. A schema is
  * named, has a type, and has several instances which can be configured
  * individually with different sources, sinks, and config overrides.
  *
  * @param detType   The type of detector this represents.
  * @param instances A list of instances, with more specific details.
  */
case class DetectorSchema(
  @JsonProperty("type")
  @JsonScalaEnumeration(classOf[DetectorTypeReference])
  detType: DetectorType.ValueBuilder,
  instances: Iterable[DetectorInstance]
) {
  def buildKeyed: Iterable[(DetectorInstance, KeyedProcessFunction[String, Measurement, Event] with HasFlinkConfig)] = {
    instances.map(inst => (inst, inst.buildKeyed(detType)))
  }

  def buildWindowed: Iterable[(DetectorInstance, ProcessWindowFunction[Measurement, Event, String, Window] with HasFlinkConfig, StreamWindowType.Value)] = {
    instances.map { inst =>
      val windowed = inst.buildWindowed(detType)
      (inst, windowed._1, windowed._2)
    }
  }
}
