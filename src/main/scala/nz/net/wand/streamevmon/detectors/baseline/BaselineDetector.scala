package nz.net.wand.streamevmon.detectors.baseline

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.traits.{HasDefault, Measurement}
import nz.net.wand.streamevmon.parameters.{HasParameterSpecs, ParameterSpec}
import nz.net.wand.streamevmon.parameters.constraints.ParameterConstraint

import java.time.{Duration, Instant}

import org.apache.commons.math3.stat.descriptive.rank.Percentile
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.util.Collector

import scala.collection.mutable

/** Simple detector that gives events when the "baseline" of a time series
  * changes significantly. The baseline is defined as the value at a certain
  * percentile over a sliding window. See the package object for configuration
  * details.
  */
class BaselineDetector[MeasT <: Measurement with HasDefault]
  extends KeyedProcessFunction[String, MeasT, Event] with HasFlinkConfig with Logging {

  final val flinkName = "Baseline Detector"
  final val flinkUid = "baseline-detector"
  final val configKeyGroup = "baseline"

  private var lastObserved: ValueState[Instant] = _

  protected var maxHistory: Int = _
  protected var percentile: Double = _
  protected var threshold: Double = _

  /** If this time passes without a new measurement, all data is dropped. */
  private var inactivityPurgeTime: Duration = _

  /** The values of the more recent measurements. */
  private var recents: ValueState[mutable.Queue[Double]] = _

  protected var lastResult: Double = 0

  @transient private var percentileCalc: Percentile = _

  override def open(parameters: Configuration): Unit = {
    lastObserved = getRuntimeContext.getState(
      new ValueStateDescriptor[Instant](
        "Last Observed Measurement Time",
        createTypeInformation[Instant]
      )
    )

    recents = getRuntimeContext.getState(
      new ValueStateDescriptor[mutable.Queue[Double]](
        "Recent Measurements",
        createTypeInformation[mutable.Queue[Double]]
      )
    )

    val config = configWithOverride(getRuntimeContext)
    maxHistory = config.getInt(s"detector.$configKeyGroup.maxHistory")
    percentile = config.getDouble(s"detector.$configKeyGroup.percentile")
    threshold = config.getDouble(s"detector.$configKeyGroup.threshold")
    inactivityPurgeTime = Duration.ofSeconds(config.getInt(s"detector.$configKeyGroup.inactivityPurgeTime"))

    percentileCalc = new Percentile(percentile)
  }

  def reset(value: MeasT): Unit = {
    if (value.isLossy) {
      lastObserved.update(null.asInstanceOf[Instant])
    }
    else {
      lastObserved.update(value.time)
      recents.update(mutable.Queue(value.defaultValue.get))
    }
  }

  override def processElement(
    value: MeasT,
    ctx  : KeyedProcessFunction[String, MeasT, Event]#Context,
    out  : Collector[Event]
  ): Unit = {
    // If there was no last value, or if it's been too long since the last
    // measurement, we reset.
    if (lastObserved.value == null ||
      Duration
        .between(lastObserved.value, value.time)
        .compareTo(inactivityPurgeTime) > 0) {
      reset(value)
      return
    }

    // If the last measurement was in the past compared to the new one, update
    // the last measurement.
    if (!Duration.between(lastObserved.value, value.time).isNegative) {
      lastObserved.update(value.time)
    }

    // If the value is lossy, we can't do anything with it.
    if (value.isLossy) {
      return
    }

    // Add the value to the queue.
    recents.value.enqueue(value.defaultValue.get)
    if (recents.value.size > maxHistory) {
      recents.value.dequeue()
    }

    val result = percentileCalc.evaluate(recents.value().toArray)

    // If we have enough values, do a comparison to see if the newest one makes for an event.
    if (recents.value.size >= maxHistory) {
      val severity = Event.changeMagnitudeSeverity(lastResult, result)
      if (severity > threshold) {
        out.collect(
          new Event(
            "baseline_events",
            value.stream,
            severity,
            value.time,
            Duration.ZERO,
            s"Observed baseline changed from $lastResult to $result",
            Map()
          )
        )
      }
    }

    lastResult = result
  }
}

object BaselineDetector extends HasParameterSpecs {
  private val maxHistorySpec = ParameterSpec(
    "detector.baseline.maxHistory",
    50,
    Some(1),
    Some(300)
  )
  private val percentileSpec = ParameterSpec(
    "detector.baseline.percentile",
    0.1,
    Some(0.0),
    Some(1.0)
  )
  private val thresholdSpec = ParameterSpec(
    "detector.baseline.threshold",
    25,
    Some(0),
    Some(100)
  )
  private val inactivityPurgeTimeSpec = ParameterSpec(
    "detector.baseline.inactivityPurgeTime",
    600,
    Some(0),
    Some(Int.MaxValue)
  )

  override val parameterSpecs: Seq[ParameterSpec[Any]] = Seq(
    maxHistorySpec,
    percentileSpec,
    thresholdSpec,
    inactivityPurgeTimeSpec
  ).asInstanceOf[Seq[ParameterSpec[Any]]]

  override val parameterRestrictions: Seq[ParameterConstraint.ComparableConstraint[Any]] = Seq()
}
