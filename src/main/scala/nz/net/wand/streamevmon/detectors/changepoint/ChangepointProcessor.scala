package nz.net.wand.streamevmon.detectors.changepoint

import nz.net.wand.streamevmon.events.ChangepointEvent
import nz.net.wand.streamevmon.measurements.Measurement

import java.time.{Duration, Instant}

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.util.Collector

import scala.language.implicitConversions

class ChangepointProcessor[MeasT <: Measurement, DistT <: Distribution[MeasT]](
    initialDistribution: DistT
) {

  implicit private def distToDistT(x: Distribution[MeasT]): DistT = x.asInstanceOf[DistT]

  private case class Run(uid: Int, dist: DistT, prob: Double, start: Instant) {

    def mergeWith(other: Run): Run = {
      if (normalIndex == uid) {
        normalIndex = other.uid
      }
      if (lastMostLikelyIndex == uid) {
        lastMostLikelyIndex = other.uid
      }
      Run(
        other.uid,
        dist,
        prob + other.prob,
        start
      )
    }
  }

  implicit private class SeqOfRuns(s: Seq[Run]) {

    def applyGrowthProbabilities(value: MeasT): Seq[Run] = {
      s.map(
        x =>
          Run(
            x.uid,
            x.dist.withPoint(value),
            if (x.dist.variance == 0) {
              x.prob
            }
            else {
              x.prob * x.dist.pdf(value) * (1 - hazard)
            },
            x.start
        )) :+ Run(
        getNewRunIndex,
        initialDistribution.withPoint(value),
        1.0,
        value.time
      )
    }

    def normalise: Seq[Run] = {
      val total = s.filterNot(_.dist.variance == 0).map(_.prob).sum
      s.map(x =>
        if (x.dist.variance == 0) {
          x
        }
        else {
          Run(
            x.uid,
            x.dist,
            x.prob / total,
            x.start
          )
      })
    }

    def squashOldRuns: Seq[Run] = {
      if (s.length > maxHistory) {
        Seq(
          s.head.mergeWith(s.drop(1).head)
        ) ++ s.drop(2)
      }
      else {
        s
      }
    }
  }

  private val hazard = 1.0 / 200.0

  /** The maximum number of runs to retain */
  private val maxHistory = 4

  /** The number of consecutive outliers that must occur before we believe that
    * the input data is behaving erratically. The sameRunConsecutiveTriggerCount
    * trigger must not be tripped for this one to trip.
    */
  private val erraticTriggerCount = 10

  /** The number of consecutive outliers that belong to the same run that must
    * occur before we believe that run is the new normal.
    */
  private val sameRunConsecutiveTriggerCount = 5

  private val inactivityPurgeTime = Duration.ofSeconds(60)

  private var runIndexCounter = 0
  private def getNewRunIndex: Int = {
    runIndexCounter += 1
    runIndexCounter - 1
  }
  private var currentRuns: Seq[Run] = _

  private var savedNormal: Run = _

  /** The last measurement we observed */
  private var lastObserved: MeasT = _

  /** The number of data points in a row that don't fit the expected data */
  private var consecutiveAnomalies: Int = _
  private var consecutiveAnomaliesSameRun: Int = _

  private var lastMostLikelyIndex: Int = _
  private var normalIndex: Int = _

  def open(config: ParameterTool): Unit = {}

  def reset(firstItem: MeasT): Unit = {
    runIndexCounter = 0
    currentRuns = Seq(
      Run(
        getNewRunIndex,
        initialDistribution.withPoint(firstItem),
        1.0,
        firstItem.time
      ))

    lastObserved = firstItem

    consecutiveAnomalies = 0
    consecutiveAnomaliesSameRun = 0
    normalIndex = currentRuns.head.uid
    lastMostLikelyIndex = normalIndex

    // We don't need to set savedNormal here since it's overwritten whenever a
    // new item is processed.
  }

  def differentEnough(oldNormal: Run, newNormal: Run): Boolean = {
    if (Math.abs(oldNormal.dist.mean - newNormal.dist.mean) > 0) {
      true
    }
    else {
      false
    }
  }

  def newEvent(out: Collector[ChangepointEvent],
               oldNormal: Run,
               newNormal: Run,
               value: MeasT): Unit = {
    out.collect(
      ChangepointEvent(
        Map("type" -> "change"),
        value.stream,
        0,
        newNormal.start,
        value.time.toEpochMilli - newNormal.start.toEpochMilli,
        "Changepoint"
      ))
  }

  // TODO: This gives an incorrect value for detection latency
  def newErraticEvent(out: Collector[ChangepointEvent], value: MeasT): Unit = {
    out.collect(
      ChangepointEvent(
        Map("type" -> "erratic"),
        value.stream,
        0,
        value.time,
        0,
        "Erratic!"
      ))
  }

  def processElement(
      value: MeasT,
      out: Collector[ChangepointEvent]
  ): Unit = {

    // If this is the first item observed, we start from fresh.
    // If it's been a while since our last measurement, our old runs probably
    // aren't much use anymore, so we should start over as well.
    if (lastObserved == null ||
        Duration
          .between(lastObserved.time, value.time)
          .compareTo(inactivityPurgeTime) > 0) {
      reset(value)
      return
    }

    // Update the last observed value if it was the most recent one seen.
    if (!Duration.between(lastObserved.time, value.time).isNegative) {
      lastObserved = value
    }

    // Save the current normal run.
    savedNormal = currentRuns.find(_.uid == normalIndex).get

    // Figure out how well the new point matches the distributions we have.
    val runsAfterGrowth = currentRuns
      .applyGrowthProbabilities(value)
    //.squashOldRuns
    //.normalise
    val mostLikelyRun = runsAfterGrowth.filterNot(_.dist.variance == 0).maxBy(_.prob)

    currentRuns = runsAfterGrowth

    // If this measurement doesn't match our current 'normal' run, update a counter.
    if (normalIndex != mostLikelyRun.uid) {
      consecutiveAnomalies += 1
      //println(s"Anomaly $consecutiveAnomalies")

      // If it's the same abnormal run as last time, there's a different counter.
      if (lastMostLikelyIndex == mostLikelyRun.uid) {
        consecutiveAnomaliesSameRun += 1
        //println(s"Same Run Consecutive Anomaly $consecutiveAnomaliesSameRun")
      }
      else {
        lastMostLikelyIndex = mostLikelyRun.uid
        consecutiveAnomaliesSameRun = 0
        //println("Different Run")
      }
    }

    // After a few consecutive values that conform better to the same run,
    // that run becomes the new 'normal'.
    if (consecutiveAnomaliesSameRun > sameRunConsecutiveTriggerCount) {
      consecutiveAnomalies = 0
      consecutiveAnomaliesSameRun = 0
      normalIndex = mostLikelyRun.uid

      //println("Trigger!")

      // If the new normal is sufficiently different from the old one, we should
      // generate an event.
      if (differentEnough(savedNormal, mostLikelyRun)) {
        newEvent(out, savedNormal, mostLikelyRun, value)
      }
    }

    // If the most likely run is changing a lot, we consider the sequence to be
    // erratic. This should generate an event.
    if (consecutiveAnomalies > erraticTriggerCount) {
      //println("Erratic trigger!")
      newErraticEvent(out, value)
      consecutiveAnomalies = 0
      consecutiveAnomaliesSameRun = 0
    }
  }
}
