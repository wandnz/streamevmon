package nz.net.wand.streamevmon.detectors.spike

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.traits.{HasDefault, Measurement}
import nz.net.wand.streamevmon.parameters.{HasParameterSpecs, ParameterSpec}
import nz.net.wand.streamevmon.parameters.constraints.ParameterConstraint

import java.time.{Duration, Instant}

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

/** This detector emits varying signals depending on how different the given
  * measurement is from recent ones. See the package object for details.
  *
  * @tparam MeasT The type of measurement to analyse.
  */
class SpikeDetector[MeasT <: Measurement with HasDefault]
  extends KeyedProcessFunction[String, MeasT, Event]
          with CheckpointedFunction
          with HasFlinkConfig {

  final val flinkName = "Spike Detector"
  final val flinkUid = "spike-detector"
  final val configKeyGroup = "spike"

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

    val config = configWithOverride(getRuntimeContext)
    inactivityPurgeTime = Duration.ofSeconds(config.getInt(s"detector.$configKeyGroup.inactivityPurgeTime"))
    lag = config.getInt(s"detector.$configKeyGroup.lag")
    threshold = config.getDouble(s"detector.$configKeyGroup.threshold")
    influence = config.getDouble(s"detector.$configKeyGroup.influence")
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
        // TODO: This could feasibly cause issues if lastObserved is lossy.
        //  Is it worth adding a `requiresNonLossy` field to HasFlinkConfig?
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
    lastObservedValue.update(value.defaultValue)
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {}

  override def initializeState(context: FunctionInitializationContext): Unit = {
    // We don't care about manually storing state, so we just enable the flag
    // that lets us get around the fact that state restoration occasionally
    // breaks queues.
    justInitialised = true
  }
}

object SpikeDetector extends HasParameterSpecs {

  private val inactivityPurgeTimeSpec = ParameterSpec(
    "detector.spike.inactivityPurgeTime",
    60,
    Some(0),
    Some(Int.MaxValue)
  )
  private val lagSpec = ParameterSpec(
    "detector.spike.lag",
    500,
    Some(0),
    Some(600)
  )
  private val thresholdSpec = ParameterSpec(
    "detector.spike.threshold",
    30.0,
    Some(Double.MinPositiveValue),
    Some(1000.0) // arbitrary
  )
  private val influenceSpec = ParameterSpec(
    "detector.spike.influence",
    0.01,
    Some(0.0),
    Some(1.0)
  )

  override val parameterSpecs: Seq[ParameterSpec[Any]] = Seq(
    inactivityPurgeTimeSpec,
    lagSpec,
    thresholdSpec,
    influenceSpec
  ).asInstanceOf[Seq[ParameterSpec[Any]]]

  override val parameterRestrictions: Seq[ParameterConstraint.ComparableConstraint[Any]] = Seq()
}
