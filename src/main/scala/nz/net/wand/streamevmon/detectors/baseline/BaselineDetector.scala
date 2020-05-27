package nz.net.wand.streamevmon.detectors.baseline

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.Measurement

import java.time.{Duration, Instant}

import org.apache.commons.math3.stat.descriptive.rank.Percentile
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.util.Collector

import scala.collection.mutable

class BaselineDetector[MeasT <: Measurement]
  extends KeyedProcessFunction[String, MeasT, Event] {

  final val detectorName = "Baseline Detector"
  final val detectorUid = "baseline-detector"

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

    val config =
      getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
    maxHistory = config.getInt("detector.baseline.maxHistory")
    percentile = config.getDouble("detector.baseline.percentile")
    threshold = config.getDouble("detector.baseline.threshold")
    inactivityPurgeTime = Duration.ofSeconds(config.getInt("detector.baseline.inactivityPurgeTime"))

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
        //println(s"[${value.time.toEpochMilli}, $lastResult, $result, ${Math.abs(lastResult - result)}, $severity],")
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
