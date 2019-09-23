package nz.net.wand.streamevmon.detectors.changepoint

import nz.net.wand.streamevmon.events.ChangepointEvent
import nz.net.wand.streamevmon.measurements.Measurement

import java.io.{File, PrintWriter}
import java.time.{Duration, Instant}

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.util.Collector

import scala.language.implicitConversions

class ChangepointProcessor[MeasT <: Measurement, DistT <: Distribution[MeasT]](
  initialDistribution: DistT,
  normalise          : Boolean,
  squash             : Boolean
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
        other.dist,
        prob + other.prob,
        other.start
      )
    }

    def isMature: Boolean = dist.n > runMaturityAge
  }

  implicit private class SeqOfRuns(s: Seq[Run]) {

    def addPoint(value                : MeasT, newRun: Boolean = false): Seq[Run] = {
      val withPoint = s.map { x =>
        Run(
          x.uid,
          x.dist.withPoint(value),
          x.prob,
          x.start
        )
      }
      if (newRun) {
        withPoint :+ Run(
          getNewRunIndex,
          initialDistribution.withPoint(value),
          1.0,
          value.time
        )
      }
      else {
        withPoint
      }
    }

    def addRuns(value: Seq[Run]): Seq[Run] = {
      s ++ value
    }

    def updateProbabilities(value: MeasT): Seq[Run] = {
      s.map(
        x => {
          //println(s"${x.uid}: ${x.dist.pdf(value)}, ${x.dist.pdf(value) / x.dist.pdf(x.dist.mean)}")
          Run(
            x.uid,
            x.dist,
            x.dist.pdf(value) * x.prob * (1 - hazard),
            x.start
          )
        }
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

  private val runMaturityAge = 20

  /** The maximum number of runs to retain */
  private val maxHistory = 20

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

  private var runIndexCounter = -1
  private def getNewRunIndex: Int = {
    runIndexCounter += 1
    runIndexCounter
  }

  private var currentRuns: Seq[Run] = Seq()
  private var immatureRuns: Seq[Run] = Seq()

  private var savedNormal: Run = _

  /** The last measurement we observed */
  private var lastObserved: MeasT = _

  /** The number of data points in a row that don't fit the expected data */
  private var consecutiveAnomalies: Int = _
  private var consecutiveAnomaliesSameRun: Int = _

  private var lastMostLikelyIndex: Int = _
  private var normalIndex: Int = _

  def open(config: ParameterTool): Unit = {
    writer.write("NewEntry,")
    writer.write("MostLikelyID,")
    Range(0, maxHistory).foreach(i => writer.write(s"uid$i,prob$i,n$i,mean$i,var$i,"))
    writer.println()
    writer.flush()
  }

  def reset(firstItem: MeasT): Unit = {
    runIndexCounter = 0
    immatureRuns = Seq().addPoint(firstItem, newRun = true)
    currentRuns = Seq()

    lastObserved = firstItem

    consecutiveAnomalies = 0
    consecutiveAnomaliesSameRun = 0
    normalIndex = -1
    lastMostLikelyIndex = -1

    // We don't need to set savedNormal here since it's overwritten whenever a
    // new item is processed.
  }

  def differentEnough(oldNormal: Run, newNormal: Run): Boolean = {
    true
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
      (currentRuns.isEmpty && immatureRuns.isEmpty) ||
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

    // Save the current normal run. If data is still immature, put a placeholder down instead.
    savedNormal = currentRuns.find(_.uid == normalIndex).getOrElse(Run(-1, initialDistribution, 0.0, Instant.EPOCH))

    // Update our playpen of immature runs. We don't need to bother applying
    // probabilities to these yet, since they're excluded from evaluation.
    val (mature, immature) = immatureRuns.addPoint(value, newRun = true).partition(_.isMature)
    immatureRuns = immature

    // If all our data is still immature, there's nothing else to do.
    if (currentRuns.isEmpty && mature.isEmpty) {
      return
    }

    // We should add the new value to the current runs so that we can then
    // update their probabilities. The newly mature runs already include the new
    // value, so we do this before including the new ones.
    currentRuns = currentRuns.addPoint(value)

    // Once a run has matured, we add it to the runs that are being evaluated.
    // It begins with the default probability (1.0).
    currentRuns = currentRuns.addRuns(mature)

    // Next, we apply growth probabilities. They depend on the value we just added.
    currentRuns = currentRuns
      .updateProbabilities(value)

    if (squash) {
      currentRuns = currentRuns.squashOldRuns
    }
    if (normalise) {
      currentRuns = currentRuns.normalise
    }

    if (currentRuns.isEmpty) {
      reset(value)
      return
    }

    val mostLikelyRun = currentRuns.maxBy(_.prob)

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
        //newEvent(out, savedNormal, mostLikelyRun, value)
      }
    }

    // If the most likely run is changing a lot, we consider the sequence to be
    // erratic. This should generate an event.
    if (consecutiveAnomalies > erraticTriggerCount) {
      //println("Erratic trigger!")
      //newErraticEvent(out, value)
      consecutiveAnomalies = 0
      consecutiveAnomaliesSameRun = 0
    }

    writeState(value, mostLikelyRun)
  }

  private val formatter: Run => String = { x =>
    s"${x.uid},${x.prob},${x.dist.n},${x.dist.mean},${x.dist.variance}"
  }

  private def writeState(value: MeasT, mostLikelyRun: Run): Unit = {
    writer.print(s"${initialDistribution.asInstanceOf[NormalDistribution[MeasT]].mapFunction(value)},")
    writer.print(s"${mostLikelyRun.uid},")
    currentRuns.foreach(x => writer.print(s"${formatter(x)},"))
    writer.println()

    writer.flush()
  }

  private val getFile = {
    s"${
      if (normalise) {
        "Normalise"
      }
      else {
        "NoNormalise"
      }
    }-${
      if (squash) {
        "Squash"
      }
      else {
        "NoSquash"
      }
    }"
  }

  private val writer = new PrintWriter(new File(s"../plot-ltsi/processed/$getFile.csv"))
}
