package nz.net.wand.streamevmon.detectors

import nz.net.wand.streamevmon.events.ChangepointEvent
import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.Logging

import java.time.Duration

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

class ChangepointDetector[MeasT <: Measurement : TypeInformation, DistT <: Distribution[MeasT]](
  initialDistribution: DistT
) extends KeyedProcessFunction[Int, MeasT, ChangepointEvent] with Logging {

  final val detectorName = s"Changepoint Detector (${initialDistribution.distributionName})"

  /** The maximum number of runs to retain */
  private val maxHistory = 10

  private val inactivityPurgeTime = Duration.ofSeconds(60)

  /** Stores tuples of a Distribution and the probability that the last point seen belongs to it. */
  private var normalRunHolder: ValueState[RunHolder[MeasT, DistT]] = _

  private def normalRuns: RunHolder[MeasT, DistT] = normalRunHolder.value

  private var currentRunHolder: ValueState[RunHolder[MeasT, DistT]] = _

  private def currentRuns: RunHolder[MeasT, DistT] = currentRunHolder.value

  private var savedNormal: ValueState[RunHolder[MeasT, DistT]] = _

  /** The last measurement we observed */
  private var lastObserved: ValueState[MeasT] = _

  /** The number of data points in a row that don't fit the expected data */
  private var consecutiveAnomalies: ValueState[Int] = _

  /** If true, then normalRuns and currentRuns are the same. ?? */
  private var normalIsCurrent: ValueState[Boolean] = _

  override def open(parameters: Configuration): Unit = {
    currentRunHolder = getRuntimeContext.getState(
      new ValueStateDescriptor[RunHolder[MeasT, DistT]]("Current Runs", createTypeInformation[RunHolder[MeasT, DistT]]))

    normalRunHolder = getRuntimeContext.getState(
      new ValueStateDescriptor[RunHolder[MeasT, DistT]]("Normal Runs", createTypeInformation[RunHolder[MeasT, DistT]]))

    savedNormal = getRuntimeContext.getState(
      new ValueStateDescriptor[RunHolder[MeasT, DistT]]("Saved Normal Runs", createTypeInformation[RunHolder[MeasT, DistT]]))

    lastObserved = getRuntimeContext.getState(
      new ValueStateDescriptor[MeasT]("Last Observed Measurement", createTypeInformation[MeasT]))

    consecutiveAnomalies = getRuntimeContext.getState(
      new ValueStateDescriptor[Int]("Consecutive Anomalies", createTypeInformation[Int]))

    normalIsCurrent = getRuntimeContext.getState(
      new ValueStateDescriptor[Boolean]("Normal Runs Is Current Runs", createTypeInformation[Boolean])
    )
  }

  override def processElement(
    value                          : MeasT,
    ctx                            : KeyedProcessFunction[Int, MeasT, ChangepointEvent]#Context,
    out                            : Collector[ChangepointEvent]
  ): Unit = {

    if (currentRunHolder.value == null) {
      currentRunHolder.update(new RunHolder[MeasT, DistT](initialDistribution))
    }
    if (normalRunHolder.value == null) {
      normalRunHolder.update(new RunHolder[MeasT, DistT](initialDistribution))
    }

    // If this is the first item observed,
    // create a new run with our new datapoint and the maximum probability.
    if (lastObserved.value == null) {
      currentRuns.add(value)
      normalIsCurrent.update(true)
      consecutiveAnomalies.update(0)
      lastObserved.update(value)
      return
    }
    // If it's been a while since our last measurement, purge all the runs
    // we've collected. They're too old to be useful.
    else if (Duration.between(lastObserved.value.time, value.time).compareTo(inactivityPurgeTime) > 0) {
      currentRuns.purge()
      normalRuns.purge()
      normalIsCurrent.update(true)
      consecutiveAnomalies.update(0)
    }

    // Update the last observed value if it was the most recent one seen.
    if (!Duration.between(lastObserved.value.time, value.time).isNegative) {
      lastObserved.update(value)
    }

    // Take a copy of the last known good normal, and put a new distribution
    // containing the new value into the relevant runs.
    if (normalIsCurrent.value) {
      savedNormal.update(currentRuns)
    }
    else {
      savedNormal.update(normalRuns)
    }

    val currentWeight = currentRuns.applyGrowthProbabilities(value)
    val normalWeight = if (normalIsCurrent.value) {
      0.0
    }
    else {
      normalRuns.applyGrowthProbabilities(value)
    }


  }
}
