package nz.net.wand.streamevmon.detectors.changepoint

import nz.net.wand.streamevmon.events.ChangepointEvent
import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.Logging

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

class ChangepointDetector[MeasT <: Measurement: TypeInformation, DistT <: Distribution[MeasT]](
  initialDistribution: DistT,
  normalise: Boolean,
  squash             : Boolean
) extends KeyedProcessFunction[Int, MeasT, ChangepointEvent]
    with Logging {

  final val detectorName = s"Changepoint Detector (${initialDistribution.distributionName})"
  final val eventDescription = s"Changepoint Event"

  private var processor: ValueState[ChangepointProcessor[MeasT, DistT]] = _

  override def open(parameters: Configuration): Unit = {
    processor = getRuntimeContext.getState(
      new ValueStateDescriptor[ChangepointProcessor[MeasT, DistT]](
        "Changepoint Processor",
        createTypeInformation[ChangepointProcessor[MeasT, DistT]]
      )
    )
  }

  override def processElement(
      value: MeasT,
      ctx: KeyedProcessFunction[Int, MeasT, ChangepointEvent]#Context,
      out: Collector[ChangepointEvent]
  ): Unit = {
    if (processor.value == null) {
      processor.update(new ChangepointProcessor[MeasT, DistT](initialDistribution, normalise, squash))
      processor.value.open(
        getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
      )
    }
    processor.value.processElement(value, out)
  }
}
