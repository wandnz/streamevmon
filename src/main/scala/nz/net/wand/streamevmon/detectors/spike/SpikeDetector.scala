package nz.net.wand.streamevmon.detectors.spike

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.Measurement

import java.time.{Duration, Instant}

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

/** This detector emits varying signals depending on how different the given
  * measurement is from recent ones.
  *
  * @tparam MeasT The type of measurement to analyse.
  */
class SpikeDetector[MeasT <: Measurement]
  extends KeyedProcessFunction[String, MeasT, Event]
          with CheckpointedFunction {

  final val detectorName = "Spike Detector"
  final val detectorUid = "spike-detector"

  private val detailedOutputTag = OutputTag[SpikeDetail]("detailed-output")

  /** The last measurement we saw. Used along with inactivityPurgeTime. */
  private var lastObserved: ValueState[Instant] = _
  private var lastObservedValue: ValueState[Option[Double]] = _

  /** This class does all the heavy lifting. */
  private var smoothedZScore: ValueState[SmoothedZScore] = _

  /** The amount of time between measurements before history is forgotten. */
  private var inactivityPurgeTime: Duration = _

  /** As described in [[SmoothedZScore]] */
  private var lag: Int = _
  /** As described in [[SmoothedZScore]] */
  private var threshold: Double = _
  /** As described in [[SmoothedZScore]] */
  private var influence: Double = _

  /** This gets set true when initializeState is called, and false when the
    * first measurement arrives. It help us avoid serialisation issues.
    */
  private var justInitialised = false

  override def open(parameters: Configuration): Unit = {
    lastObserved = getRuntimeContext.getState(
      new ValueStateDescriptor[Instant](
        "Last Observed Measurement Time",
        TypeInformation.of(classOf[Instant])
      )
    )
    lastObservedValue = getRuntimeContext.getState(
      new ValueStateDescriptor[Option[Double]](
        "Last Observed Measurement Value",
        TypeInformation.of(classOf[Option[Double]])
      )
    )

    smoothedZScore = getRuntimeContext.getState(
      new ValueStateDescriptor[SmoothedZScore](
        "Smoothed Z Score",
        TypeInformation.of(classOf[SmoothedZScore])
      )
    )

    val config =
      getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
    inactivityPurgeTime = Duration.ofSeconds(config.getInt("detector.spike.inactivityPurgeTime"))
    lag = config.getInt("detector.spike.lag")
    threshold = config.getDouble("detector.spike.threshold")
    influence = config.getDouble("detector.spike.influence")
  }

  /** Resets the state of the detector. Happens on the first measurement, and
    * any time there's been a reasonable amount of time between the last one and
    * a new one.
    */
  def reset(value: MeasT): Unit = {
    lastObserved.update(value.time)
    lastObservedValue.update(value.defaultValue)
    smoothedZScore.update(SmoothedZScore(lag, threshold, influence))
  }

  override def processElement(
    value: MeasT,
    ctx  : KeyedProcessFunction[String, MeasT, Event]#Context,
    out  : Collector[Event]
  ): Unit = {
    // If this is the first measurement or it's been too long since the last one,
    // we'll reset everything.
    if (lastObserved.value == null ||
      (!inactivityPurgeTime.isZero &&
        Duration
          .between(lastObserved.value, value.time)
          .compareTo(inactivityPurgeTime) > 0)) {
      reset(value)
      return
    }

    if (!Duration.between(lastObserved.value, value.time).isNegative) {
      lastObserved.update(value.time)
      lastObservedValue.update(value.defaultValue)
    }

    if (value.isLossy) {
      return
    }

    // Recreate the queue if needed
    if (justInitialised) {
      smoothedZScore.value.refreshState()
      justInitialised = false
    }

    val mean = smoothedZScore.value.lastMean
    val std = smoothedZScore.value.lastStd

    // Add the value into the queue, and get a signal result.
    // We can't use the result enum here for some reason, but that's okay.
    val signal = smoothedZScore.value.addValue(value.defaultValue.get)

    ctx.output(
      detailedOutputTag,
      SpikeDetail(
        value.defaultValue.get,
        mean,
        value.defaultValue.get - (threshold * std),
        value.defaultValue.get + (threshold * std),
        Math.abs(value.defaultValue.get - mean) + value.defaultValue.get,
        signal
      )
    )
    signal match {
      case SignalType.NoSignal =>
      case SignalType.Negative | SignalType.Positive =>
        // This could feasibly cause issues if lastObserved is lossy, but all
        // the implementations at time of writing are guarded by a filter for
        // those measurements.
        val severity = Event.changeMagnitudeSeverity(lastObservedValue.value.get, value.defaultValue.get)

        out.collect(
          new Event(
            "spike_events",
            value.stream,
            severity,
            value.time,
            Duration.ZERO,
            signal.toString,
            Map()
          )
        )
    }
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {}

  override def initializeState(context: FunctionInitializationContext): Unit = {
    // We don't care about manually storing state, so we just enable the flag
    // that lets us get around the fact that state restoration occasionally
    // breaks queues.
    justInitialised = true
  }
}
