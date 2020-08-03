package nz.net.wand.streamevmon.detectors.changepoint

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.{HasDefault, Measurement}
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.flink.HasFlinkConfig

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
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
class ChangepointDetector[
  MeasT <: Measurement with HasDefault : TypeInformation,
  DistT <: Distribution[MeasT] : TypeInformation
](
  initialDistribution: DistT,
  shouldDoGraphs     : Boolean = false,
  filename           : Option[String] = None
) extends KeyedProcessFunction[String, MeasT, Event]
          with HasFlinkConfig
          with Logging {

  final val flinkName = s"Changepoint Detector (${initialDistribution.distributionName})"
  final val flinkUid = s"changepoint-detector-${initialDistribution.distributionName}"
  final val configKeyGroup = "changepoint"

  private var processor: ValueState[ChangepointProcessor[MeasT, DistT]] = _

  override def open(parameters: Configuration): Unit = {
    processor = getRuntimeContext.getState(
      new ValueStateDescriptor[ChangepointProcessor[MeasT, DistT]](
        "Changepoint Processor",
        TypeInformation.of(classOf[ChangepointProcessor[MeasT, DistT]])
      )
    )
  }

  override def processElement(
    value: MeasT,
    ctx  : KeyedProcessFunction[String, MeasT, Event]#Context,
    out  : Collector[Event]
  ): Unit = {
    if (processor.value == null) {
      processor.update(new ChangepointProcessor[MeasT, DistT](initialDistribution, configKeyGroup, shouldDoGraphs, filename))
      processor.value.open(configWithOverride(getRuntimeContext))
    }
    processor.value.processElement(value, out)
  }
}
