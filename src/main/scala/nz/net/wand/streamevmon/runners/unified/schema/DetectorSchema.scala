package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.detectors.HasFlinkConfig
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.Measurement

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction

/** An entry in the `detectors` key of the yaml configuration. Can be built,
  * resulting in an Iterable of all represented detectors.
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
  def build: Iterable[(DetectorInstance, KeyedProcessFunction[String, Measurement, Event] with HasFlinkConfig)] = {
    instances.map(inst => (inst, inst.build(detType)))
  }
}
