package nz.net.wand.streamevmon.detectors

import nz.net.wand.streamevmon.events.ChangepointEvent
import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.Logging

import java.time.{Duration, Instant}

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
  final val eventDescription = s"Changepoint Event"

  /** The maximum number of runs to retain */
  private val maxHistory = 10

  /** The number of consecutive outliers before we believe an event has occurred. */
  private val triggerCount = 5

  private val runFluctuationThreshold = 0.5

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

  private var lastMostLikelyNormal: ValueState[Int] = _
  private var lastMostLikelyCurrent: ValueState[Int] = _

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
      new ValueStateDescriptor[Boolean]("Normal Runs Is Current Runs", createTypeInformation[Boolean]))

    lastMostLikelyNormal = getRuntimeContext.getState(
      new ValueStateDescriptor[Int]("Last Most Likely Normal", createTypeInformation[Int]))

    lastMostLikelyCurrent = getRuntimeContext.getState(
      new ValueStateDescriptor[Int]("Last Most Likely Current", createTypeInformation[Int]))
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
      lastMostLikelyNormal.update(0)
      lastMostLikelyCurrent.update(0)
      return
    }
    // If it's been a while since our last measurement, purge all the runs
    // we've collected. They're too old to be useful.
    else if (Duration.between(lastObserved.value.time, value.time).compareTo(inactivityPurgeTime) > 0) {
      currentRuns.purge()
      normalRuns.purge()
      normalIsCurrent.update(true)
      consecutiveAnomalies.update(0)
      lastMostLikelyNormal.update(0)
      lastMostLikelyCurrent.update(0)
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

    // Update the runholders with the new value. This also adjusts the weights.
    // We then prune runs if there are too many of them, and normalise weights.
    // Then get the new most probable run index.
    currentRuns.applyGrowthProbabilities(value)
    currentRuns.pruneRuns(maxHistory)
    currentRuns.normalise()

    if (lastMostLikelyCurrent.value >= maxHistory - 1) {
      lastMostLikelyCurrent.update(lastMostLikelyCurrent.value)
    }
    val mostLikelyCurrent = currentRuns.getMostProbable
    var mostLikelyNormal = -1

    // If the probabilities are borked and everything says 0, then trigger a
    // changepoint.
    if (mostLikelyCurrent == -1) {
      currentRuns.setProbabilityToMax(0)
    }

    if (!normalIsCurrent.value) {
      normalRuns.applyGrowthProbabilities(value)
      normalRuns.pruneRuns(maxHistory)
      normalRuns.normalise()
      if (lastMostLikelyNormal.value >= maxHistory - 1) {
        lastMostLikelyNormal.update(lastMostLikelyNormal.value)
      }
      mostLikelyNormal = normalRuns.getMostProbable
      if (mostLikelyNormal == -1) {
        normalRuns.setProbabilityToMax(0)
      }
    }

    // Check if the most likely run has changed
    if ((if (normalIsCurrent.value) {
      mostLikelyNormal
    }
    else {
      mostLikelyCurrent
    }) != lastMostLikelyNormal.value + 1) {
      // If so, we have a datapoint that doesn't fit the current 'normal' distribution.
      consecutiveAnomalies.update(consecutiveAnomalies.value + 1)

      // Save the last known 'normal' result
      normalRunHolder.update(savedNormal.value)
      if (normalIsCurrent.value) {
        lastMostLikelyNormal.update(lastMostLikelyCurrent.value)
      }
      lastMostLikelyCurrent.update(mostLikelyCurrent)
      normalIsCurrent.update(false)

      if (consecutiveAnomalies.value > triggerCount) {
        // Only bother sending an event if the change is significant, but swap
        // to our new normal run regardless.
        if (Math.abs(mostLikelyCurrent - (lastMostLikelyNormal.value + consecutiveAnomalies.value)) >= lastMostLikelyNormal.value * runFluctuationThreshold) {
          // TODO: Magic numbers!
          if (getEventWeight(mostLikelyCurrent) > 20) {
            newEvent(
              out,
              value,
              value.time,
              currentRuns.runs(consecutiveAnomalies.value / 2)._1.mean,
              normalRuns.runs.head._1.mean
            )
          }
        }

        currentRuns.trim(mostLikelyCurrent)

        // This is now considered the new normal.
        lastMostLikelyNormal.update(mostLikelyCurrent)
        normalIsCurrent.update(true)
        consecutiveAnomalies.update(0)
      }
    }
    else {
      if (normalIsCurrent.value) {
        // Nothing has changed
        lastMostLikelyCurrent.update(mostLikelyCurrent)
      }
      else {
        // The normal is now current again
        normalIsCurrent.update(true)
        currentRunHolder.update(normalRunHolder.value)
        lastMostLikelyCurrent.update(mostLikelyNormal)
      }
      lastMostLikelyNormal.update(lastMostLikelyCurrent.value)
      consecutiveAnomalies.update(0)
    }
  }

  def getEventWeight(mostLikelyCurrent: Int): Double = {
    val oldMaxLocation = Math.min(currentRuns.length, lastMostLikelyNormal.value + consecutiveAnomalies.value)
    val run_difference = Math.abs(consecutiveAnomalies.value + lastMostLikelyNormal.value - mostLikelyCurrent)
    val event_weight = Math.log(run_difference * (currentRuns.runs(mostLikelyCurrent)._2 / currentRuns.runs(oldMaxLocation)._2))

    logger.info(s"Event weight: $event_weight")


    event_weight
  }

  /** This is even more magic than some of the other numbers!
    */
  def eventMagnitudeLatency(d: Double, d1: Double): Int = {
    val max = Math.max(d, d1)
    val min = Math.min(d, d1)

    var basemag = if (min < 0.1) {
      4.8
    }
    else {
      Math.exp(-0.17949 * Math.log(min) + 1.13489)
    }
    if (basemag < 1.1) {
      basemag = 1.1
    }

    var mag = 30 * ((max - min) / ((basemag - 1) * min))

    logger.info(s"Mag: $mag")

    if (mag < 1) {
      mag = 1.0
    }
    if (mag > 100) {
      mag = 100.0
    }
    mag.toInt
  }

  def newEvent(
    out                                       : Collector[ChangepointEvent],
    value                                     : MeasT,
    eventTime                                 : Instant,
    oldmean                                   : Double,
    newmean                                   : Double
  ): Unit = {

    var severity = 0

    val changeDirection = if (oldmean < newmean) {
      "increased"
    }
    else {
      "decreased"
    }
    val description = if (newmean < 0) {
      severity = 80
      "Lost measurement data"
    }
    else if (oldmean < 0) {
      severity = 30
      "End of measurement loss"
    }
    else {
      severity = eventMagnitudeLatency(oldmean, newmean)
      s"Average latency $changeDirection from $oldmean to $newmean"
    }

    out.collect(ChangepointEvent(
      Map(),
      value.stream,
      severity,
      eventTime,
      value.time.toEpochMilli - eventTime.toEpochMilli,
      description
    ))
  }
}
