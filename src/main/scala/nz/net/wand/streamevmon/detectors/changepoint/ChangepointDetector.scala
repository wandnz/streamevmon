package nz.net.wand.streamevmon.detectors.changepoint

import nz.net.wand.streamevmon.events.ChangepointEvent
import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.Logging

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

/** Wrapper class for [[ChangepointProcessor]].
  * This part does Flink stuff, and stores the ChangepointProcessor in a way that
  * Flink should be able to serialise to restore saved state.
  *
  * @param initialDistribution The distribution that should be used as a base
  *                            when adding new measurements to the runs.
  * @param shouldDoGraphs      When true, .csv files are output into the ./out/graphs
  *                            directory which can be used to produce graphs of the
  *                            state of the detector. Defaults to false.
  * @param filename            When shouldDoGraphs is true, this filename is combined with
  *                            the values of the parameters set in the global configuration
  *                            to create the filename of the .csv files output.
  * @tparam MeasT The type of [[nz.net.wand.streamevmon.measurements.Measurement Measurement]] we're receiving.
  * @tparam DistT The type of [[Distribution]] to model recent measurements with.
  */
class ChangepointDetector[MeasT <: Measurement, DistT <: Distribution[MeasT]](
  initialDistribution: DistT,
  shouldDoGraphs     : Boolean = false,
  filename           : Option[String] = None
) extends KeyedProcessFunction[Int, MeasT, ChangepointEvent]
    with Logging {

  final val detectorName = s"Changepoint Detector (${initialDistribution.distributionName})"
  final val eventDescription = s"Changepoint Event"

  private var processor: ValueState[ChangepointProcessor[MeasT, DistT]] = _

  var counter: ValueState[Int] = _

  override def open(parameters: Configuration): Unit = {
    processor = getRuntimeContext.getState(
      new ValueStateDescriptor[ChangepointProcessor[MeasT, DistT]](
        "Changepoint Processor",
        TypeInformation.of(classOf[ChangepointProcessor[MeasT, DistT]])
      )
    )

    counter = getRuntimeContext.getState(
      new ValueStateDescriptor[Int](
        "Counter",
        TypeInformation.of(classOf[Int])
      )
    )
  }

  override def processElement(
      value: MeasT,
      ctx: KeyedProcessFunction[Int, MeasT, ChangepointEvent]#Context,
      out: Collector[ChangepointEvent]
  ): Unit = {
    counter.update(counter.value + 1)
    if (value.stream == 3) {
      logger.info(s"Outer counter: ${counter.value}")
    }

    if (processor.value == null) {
      processor.update(new ChangepointProcessor[MeasT, DistT](initialDistribution, shouldDoGraphs, filename))
      processor.value.open(
        getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
      )
    }
    if (!processor.value.isOpen) {
      logger.error("Calling processElement without opening first!?!")
    }
    processor.value.processElement(value, out)
  }
}
