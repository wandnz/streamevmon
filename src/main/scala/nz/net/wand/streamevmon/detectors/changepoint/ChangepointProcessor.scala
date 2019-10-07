package nz.net.wand.streamevmon.detectors.changepoint

import nz.net.wand.streamevmon.events.ChangepointEvent
import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.Logging

import java.io.{File, PrintWriter}
import java.time.{Duration, Instant}

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.util.Collector

import scala.language.implicitConversions

class ChangepointProcessor[MeasT <: Measurement, DistT <: Distribution[MeasT]](
  initialDistribution: DistT,
  shouldNormalise    : Boolean,
  shouldSquash       : Boolean
) extends Logging {

  implicit private def distToDistT(x: Distribution[MeasT]): DistT = x.asInstanceOf[DistT]

  private case class Run(uid: Int, dist: DistT, prob: Double, start: Instant) {
    def mergeWith(other: Run): Run = {
      /*
      if (normalIndex == uid) {
        normalIndex = other.uid
      }
      if (lastMostLikelyIndex == uid) {
        lastMostLikelyIndex = other.uid
      }
       */
      Run(
        other.uid,
        other.dist,
        prob + other.prob,
        other.start
      )
    }

    def isMature: Boolean = true
  }

  implicit private class SeqOfRuns(s: Seq[Run]) {
    def copy: Seq[Run] = s.map(identity)

    def addPoint(value: MeasT, newRun: Run, addNewRun: Boolean = true): Seq[Run] = {
      /*
      val withPoint = s.map { x =>
        Run(
          x.uid,
          x.dist.withPoint(value),
          x.prob,
          x.start
        )
      }
      if (addNewRun) {
        withPoint :+ newRunFor(value)
      }
      else {
        withPoint
      }
      */

      s :+ newRun
    }

    def addRuns(value: Seq[Run]): Seq[Run] = {
      s ++ value
    }

    def updateProbabilities(value: MeasT, newRun: Run): Seq[Run] = {
      /* Attempt 1
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
      */

      /* Attempt 2
      var newSeq = Seq[Run](s.last)
      var totalProb = 0.0
      for (i <- Range(s.size, 0, -1)) {
        if (i != s.size) {
          val meanPdf = s(i).dist.pdf(s(i).dist.mean)
          //totalProb += (s(i).dist.pdf(value) / meanPdf) * s(i).prob * hazard
          totalProb += s(i).dist.pdf(value) * s(i).prob * hazard

          newSeq = Run(
            newSeq.head.uid,
            newSeq.head.dist,
            //(s(i).dist.pdf(value) / meanPdf) * s(i).prob * (1 - hazard),
            s(i).dist.pdf(value) * s(i).prob * (1 - hazard),
            newSeq.head.start
          ) +: newSeq.drop(1)

          newSeq = s(i) +: newSeq
        }
      }

      Run(
        newSeq.head.uid,
        newSeq.head.dist,
        totalProb,
        newSeq.head.start
      ) +: newSeq.drop(1)
       */

      /* Attempt 3 */
      var current_runs = s
      var current_weight = 0.0

      (current_runs.length - 2 to 0 by -1).foreach { r =>
        current_weight += current_runs(r).dist.pdf(value) * current_runs(r).prob * hazard

        current_runs = current_runs.updated(r + 1, Run(
          current_runs(r + 1).uid,
          current_runs(r).dist.withPoint(value),
          current_runs(r).dist.pdf(value) * current_runs(r).prob * (1 - hazard),
          current_runs(r + 1).start
        ))
      }

      val result2 = Run(
        newRun.uid,
        newRun.dist,
        current_weight,
        newRun.start
      ) +: current_runs.drop(1)

      result2
    }

    def squashOldRuns: Seq[Run] = {
      if (shouldSquash) {
        var newRuns = s
        while (newRuns.length > maxHistory) {
          //s.dropRight(2) :+ s.last.mergeWith(s.dropRight(1).last)
          //Seq(s.head.mergeWith(s.drop(1).head)) ++ s.drop(2)
          val maxhist = newRuns(maxHistory)
          val maxhistMinusOne = newRuns(maxHistory - 1)
          newRuns = newRuns.dropRight(2) :+ Run(
            maxhist.uid,
            maxhist.dist,
            maxhist.prob + maxhistMinusOne.prob,
            maxhist.start
          )
        }
        newRuns
      }
      else {
        s
      }
    }

    /** This function definitely matches netevmon */
    def normalise: Seq[Run] = {
      if (shouldNormalise) {
        val total = s.filterNot(_.dist.variance == 0).map(_.prob).sum

        // Special case here, where the probabilities have all gone to zero due
        // to the PDF of all runs or something else. We'll just move all our
        // probability to the first run.
        if (total == 0) {
          logger.info("All probabilities are 0!")
          Run(s.head.uid,
            s.head.dist,
            1.0,
            s.head.start
          ) +: s.map(x => Run(
            x.uid,
            x.dist,
            0.0,
            x.start
          )).drop(1)
        }
        else {
          s.map(x =>
            Run(
              x.uid,
              x.dist,
              x.prob / total,
              x.start
            )
          )
        }
      }
      else {
        s
      }
    }

    def update(newlyMatureRuns: Seq[Run], value: MeasT): Seq[Run] = {
      val newRun = newRunFor(value)

      s
        // We should add the new value to the current runs so that we can then
        // update their probabilities. The newly mature runs already include the new
        // value, so we do this before including them.
        .addPoint(value, newRun)
        // Once a run has matured, we add it to the runs that are being evaluated.
        // It begins with the default probability (1.0).
        .addRuns(newlyMatureRuns)
        // Next, we apply growth probabilities. They depend on the value we just added.
        .updateProbabilities(value, newRun)
        // If there's too many runs, we condense them since some won't be useful anymore.
        .squashOldRuns
        // Finally, we make all the probabilities in the vector add to 1.
        .normalise

      val a = s.addPoint(value, newRun)
      val b = a.addRuns(newlyMatureRuns)
      val c = b.updateProbabilities(value, newRun)
      val d = c.squashOldRuns
      val e = d.normalise

      e
    }

    def trim(index: Int): Seq[Run] = s
  }

  private def newRunFor(value: MeasT): Run = {
    Run(
      getNewRunIndex,
      initialDistribution.withPoint(value),
      1.0,
      value.time
    )
  }

  private val hazard = 1.0 / 200.0

  private val runMaturityAge = -1

  /** The maximum number of runs to retain */
  private val maxHistory = 20

  /** The number of consecutive outliers that belong to the same run that must
    * occur before we believe that run is the new normal.
    */
  private val sameRunConsecutiveTriggerCount = 10

  /** The number of consecutive outliers that must occur before we believe that
    * the input data is behaving erratically. The sameRunConsecutiveTriggerCount
    * trigger must not be tripped for this one to trip.
    */
  private val erraticTriggerCount = 2 * sameRunConsecutiveTriggerCount

  private val inactivityPurgeTime = Duration.ofSeconds(60)

  private var runIndexCounter = -1
  private def getNewRunIndex: Int = {
    runIndexCounter += 1
    runIndexCounter
  }

  private var currentRuns: Seq[Run] = Seq()
  private var immatureRuns: Seq[Run] = Seq()
  private var normalRuns: Seq[Run] = Seq()
  private var normalIsCurrent: Boolean = true
  private var normalnormalindex: Int = _

  private var savedNormal: Run = _

  /** The last measurement we observed */
  private var lastObserved: MeasT = _

  /** The number of data points in a row that don't fit the expected data */
  private var consecutiveAnomalies: Int = _
  private var consecutiveAnomaliesSameRun: Int = _

  private var lastMostLikelyIndex: Int = _
  private var prev_normal_max: Int = _
  private var prev_current_max: Int = _

  private val fakeRun = Run(-1, initialDistribution, 0.0, Instant.EPOCH)

  def open(config: ParameterTool): Unit = {
    writer.write("NewEntry,")
    writer.write("MostLikelyID,")
    writer.write("NormalIsCurrent,")
    if (shouldSquash) {
      (0 to maxHistory + 4).foreach(i => writer.write(s"uid$i,prob$i,n$i,mean$i,var$i,"))
    }
    else {
      (0 to 510).foreach(i => writer.write(s"uid$i,prob$i,n$i,mean$i,var$i,"))
    }
    writer.println()
    writer.flush()

    writerPdf.write("NewEntry,")
    if (shouldSquash) {
      (0 to maxHistory + 4).foreach(i => writerPdf.write(s"pdf$i,"))
    }
    else {
      (0 to 510).foreach(i => writerPdf.write(s"pdf$i,"))
    }
    writerPdf.println()
    writerPdf.flush()
  }

  def reset(firstItem: MeasT): Unit = {
    runIndexCounter = 0
    immatureRuns = Seq().addPoint(firstItem, newRunFor(firstItem), addNewRun = true)
    currentRuns = Seq()
    normalIsCurrent = true
    normalRuns = Seq()
    normalnormalindex = 0

    lastObserved = firstItem

    consecutiveAnomalies = 0
    consecutiveAnomaliesSameRun = 0
    prev_normal_max = 0
    prev_current_max = 0
    lastMostLikelyIndex = 0

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

    println(s"Event! oldNormal: $oldNormal, newNormal: $newNormal, value: $value")

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

    // Update our playpen of immature runs. We don't need to bother applying
    // probabilities to these yet, since they're excluded from evaluation.
    val (mature, immature) = immatureRuns.addPoint(value, newRunFor(value), addNewRun = true).partition(_.isMature)
    immatureRuns = immature

    // If all our data is still immature, there's nothing else to do.
    if (currentRuns.isEmpty && mature.isEmpty) {
      return
    }

    /*
    if (currentRuns.length + mature.length <= 1) {
      currentRuns = currentRuns.addRuns(mature)
      return
    }
    */

    // Save the current normal run. If data is still immature, put a placeholder down instead.
    savedNormal = if (currentRuns.isDefinedAt(prev_normal_max)) {
      currentRuns(prev_normal_max)
    }
    else {
      fakeRun
    }

    val savedNormalRuns = if (normalIsCurrent) {
      currentRuns.copy
    }
    else {
      normalRuns.copy
    }

    currentRuns = currentRuns.update(mature, value)
    if (!normalIsCurrent) {
      normalRuns = normalRuns.update(mature, value)
    }

    // If this happens, the squashing algorithm is a little overzealous.
    if (currentRuns.isEmpty) {
      logger.warn("There were no mature runs after applying growth probabilities! Resetting detector.")
      reset(value)
      return
    }

    val mostLikelyRun = currentRuns.maxBy(_.prob)
    val current_maxI = currentRuns.zipWithIndex.maxBy(_._1.prob)._2

    val mostLikelyNormalRun = if (normalRuns.nonEmpty) {
      normalRuns.maxBy(_.prob)
    }
    else {
      fakeRun
    }
    val normal_maxI = if (normalRuns.nonEmpty) {
      normalRuns.zipWithIndex.maxBy(_._1.prob)._2
    }
    else {
      -1
    }
    if (prev_current_max >= maxHistory - 1) {
      prev_current_max -= 1
    }
    if (prev_normal_max >= maxHistory - 1) {
      prev_normal_max -= 1
    }

    // If this measurement doesn't match our current 'normal' run, update a counter.
    if ((!normalIsCurrent && current_maxI != prev_normal_max + 1) ||
      (normalIsCurrent && normal_maxI != prev_normal_max + 1)) {

      consecutiveAnomalies += 1
      //println(s"Anomaly $consecutiveAnomalies")

      normalRuns = savedNormalRuns.copy

      if (normalIsCurrent) {
        prev_normal_max = prev_current_max
      }
      prev_current_max = current_maxI
      normalIsCurrent = false
    }
    else {
      if (normalIsCurrent) {
        prev_current_max = current_maxI
      }
      else {
        normalIsCurrent = true
        currentRuns = normalRuns.copy
        prev_current_max = normal_maxI
      }

      prev_normal_max = prev_current_max
      consecutiveAnomalies = 0
    }

    if (consecutiveAnomalies > sameRunConsecutiveTriggerCount) {
      newEvent(out, currentRuns(prev_current_max), currentRuns(current_maxI), value)
      consecutiveAnomalies = 0
      consecutiveAnomaliesSameRun = 0

      currentRuns.trim(current_maxI)

      prev_normal_max = current_maxI
      normalIsCurrent = true
      consecutiveAnomalies = 0
    }

    writeState(value, mostLikelyRun)
  }

  private def writeState(value: MeasT, mostLikelyRun: Run): Unit = {
    val formatter: Run => String = x => s"${x.uid},${x.prob},${x.dist.n},${x.dist.mean},${x.dist.variance}"

    writer.print(s"${initialDistribution.asInstanceOf[NormalDistribution[MeasT]].mapFunction(value)},")
    writer.print(s"$prev_normal_max,")
    writer.print(s"$normalIsCurrent,")
    currentRuns.foreach(x => writer.print(s"${formatter(x)},"))
    writer.println()
    writer.flush()

    writerPdf.print(s"${initialDistribution.asInstanceOf[NormalDistribution[MeasT]].mapFunction(value)},")
    currentRuns.foreach(x => writerPdf.print(s"${x.dist.pdf(value)},"))
    writerPdf.println()
    writerPdf.flush()
  }

  private val getFile = {
    s"${
      if (shouldNormalise) {
        "Normalise"
      }
      else {
        "NoNormalise"
      }
    }-${
      if (shouldSquash) {
        "Squash"
      }
      else {
        "NoSquash"
      }
    }"
  }

  private val writer = new PrintWriter(new File(s"../plot-ltsi/processed/$getFile.csv"))
  private val writerPdf = new PrintWriter(new File(s"../plot-ltsi/processed/pdf/$getFile-pdf.csv"))
}
