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
  initialDistribution: DistT
) extends Logging {

  implicit private def distToDistT(x: Distribution[MeasT]): DistT = x.asInstanceOf[DistT]

  private case class Run(uid: Int, dist: DistT, prob: Double, start: Instant)

  implicit private class SeqOfRuns(s: Seq[Run]) {
    def copy: Seq[Run] = s.map(identity)

    def addRun(value: MeasT, newRun: Run): Seq[Run] = {
      s :+ newRun
    }

    def addRuns(value: Seq[Run]): Seq[Run] = {
      s ++ value
    }

    def updateProbabilities(value: MeasT, newRun: Run): Seq[Run] = {
      var current_runs = s
      var current_weight = 0.0

      (current_runs.length - 2 to 0 by -1).foreach { r =>
        current_weight += current_runs(r).dist.pdf(value) * current_runs(r).prob * hazard

        current_runs = current_runs.updated(r + 1, Run(
          current_runs(r + 1).uid,
          current_runs(r).dist.withPoint(value, r + 1),
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
      var newRuns = s
      while (newRuns.length > maxHistory) {
        val maxhist = newRuns.last
        val maxhistMinusOne = newRuns.dropRight(1).last
        newRuns = newRuns.dropRight(2) :+ Run(
          maxhistMinusOne.uid,
          maxhistMinusOne.dist,
          maxhist.prob + maxhistMinusOne.prob,
          maxhistMinusOne.start
        )
      }
      newRuns
    }

    def normalise: Seq[Run] = {
      val total = s.map(_.prob).sum

      // Special case here, where the probabilities have all gone to zero due
      // to the PDF of all runs or something else. We'll just move all our
      // probability to the first run.
      if (total == 0) {
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

    def update(value: MeasT): Seq[Run] = {
      val newRun = newRunFor(value)

      // Add a new run created from the new measurement.
      val a = s.addRun(value, newRun)
      // Apply probability changes according to the value of the new measurement.
      val c = a.updateProbabilities(value, newRun)
      // Condense old runs to comply with the maxRuns setting.
      val d = c.squashOldRuns
      // Normalise the probabilities so they all add to 1.
      val e = d.normalise

      e
    }

    def filteredMaxBy(func: Run => Double): Int = {
      // We discount the newly added run because its probability will be way
      // too high until it gets another iteration or two under its belt.
      if (s.length > 1) {
        s.dropRight(1).zipWithIndex.maxBy(x => func(x._1))._2
      }
      else {
        0
      }
    }
  }

  private def newRunFor(value: MeasT): Run = {
    Run(
      getNewRunIndex,
      initialDistribution.withPoint(value, 1),
      1.0,
      value.time
    )
  }

  // == Configurable options ==

  private val hazard = 1.0 / 200.0

  /** The maximum number of runs to retain */
  private val maxHistory = 20

  /** The number of consecutive outliers that belong to the same run that must
    * occur before we believe that run is the new normal and an event may have
    * occurred.
    */
  private val changepointTriggerCount = 10
  /** If an outlier value is followed by more than this many normal values,
    * we should ignore the outlier.
    */
  private val ignoreOutlierAfterNormalMeasurementCount = 1

  private val inactivityPurgeTime = Duration.ofSeconds(60)
  private val minimumEventInterval = Duration.ofSeconds(10)

  private val severityThreshold = 30

  // == End of configurable options ==

  private var runIndexCounter = -1
  private def getNewRunIndex: Int = {
    runIndexCounter += 1
    runIndexCounter
  }

  private var currentRuns: Seq[Run] = Seq()
  private var normalRuns: Seq[Run] = Seq()
  private var magicFlagOfGraphing: Boolean = true

  /** The last measurement we observed */
  private var lastObserved: MeasT = _
  private var lastEventTime: Option[Instant] = None

  /** The number of data points in a row that don't fit the expected data */
  private var consecutiveAnomalies: Int = _
  private var consecutiveOutliers: Int = _

  private var previousMostLikelyIndex: Int = _

  private var compositeOldNormal: Run = _

  private val fakeRun = Run(-1, initialDistribution, 0.0, Instant.EPOCH)

  def reset(firstItem: MeasT): Unit = {
    runIndexCounter = -1
    currentRuns = Seq()
    magicFlagOfGraphing = true

    lastObserved = firstItem

    consecutiveAnomalies = 0
    consecutiveOutliers = 0
    previousMostLikelyIndex = 0
  }

  def getSeverity(oldNormal    : Run, newNormal: Run): Int = {
    println(s"Old normal: $oldNormal")
    println(s"New normal: $newNormal")
    val absDiff = Math.abs(oldNormal.dist.mean - newNormal.dist.mean)
    val relativeDiff = absDiff / Math.min(oldNormal.dist.mean, newNormal.dist.mean)
    val normalRelDiff = if (relativeDiff > 1.0) {
      1 - (1 / relativeDiff)
    }
    else {
      relativeDiff
    }
    (normalRelDiff * 100).toInt
  }

  // TODO: Calculate times and detection latency correctly
  def newEvent(out: Collector[ChangepointEvent],
               oldNormal: Run,
               newNormal: Run,
               value: MeasT,
               severity: Int
  ): Unit = {
    if (Duration
      .between(value.time, lastEventTime.getOrElse(value.time))
      .compareTo(minimumEventInterval) > 0) {

      lastEventTime = Some(value.time)
    }


    out.collect(
      ChangepointEvent(
        Map("type" -> "changepoint"),
        value.stream,
        severity,
        oldNormal.start,
        value.time.toEpochMilli - oldNormal.start.toEpochMilli,
        s"Latency ${
          if (oldNormal.dist.mean > newNormal.dist.mean) {
            "decreased"
          }
          else {
            "increased"
          }
        } from ${oldNormal.dist.mean.toInt} to ${newNormal.dist.mean.toInt}"
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

    // If we're in a normal state, we'll update our note of our normal-state
    // runs. We'll also make a composite of the most recent time and the
    // mean with the most data in it in case there's a changepoint after this,
    // so we can use the nicest numbers for determining if it's different enough
    // and to show the end-user.
    if (consecutiveAnomalies == 0) {
      normalRuns = currentRuns.copy
      compositeOldNormal = if (normalRuns.nonEmpty) {
        Run(
          -2,
          normalRuns(normalRuns.filteredMaxBy(_.dist.n)).dist,
          -2.0,
          normalRuns(previousMostLikelyIndex).start
        )
      }
      else {
        fakeRun
      }
    }

    // Process the new value: Create a new run, adjust the current runs, and
    // update the probabilities of the runs depending on the new measurement.
    currentRuns = currentRuns.update(value)

    // If this happens, something is probably wrong with the squashing algorithm.
    if (currentRuns.isEmpty) {
      logger.error("There were no runs left after applying growth probabilities! Resetting detector.")
      reset(value)
      return
    }

    // Find the most likely run, discounting the newly added one.
    val mostLikelyIndex = currentRuns.filteredMaxBy(_.prob)

    // If the most likely run has changed, then the measurement doesn't match
    // our current model of the recent 'normal' behaviour of the measurements.
    // We'll update a counter to see if this happens for a while.
    if (mostLikelyIndex != previousMostLikelyIndex) {
      consecutiveAnomalies += 1

      // This metric can determine if the measurements have returned to normal
      // after a small number of outliers. If they have, we return the detector
      // state to how it was before the outlier occurred and disregard
      // outlier measurements. The current measurement, which follows
      // our normal trend, is included.
      val highestPdf = currentRuns.filteredMaxBy(_.dist.pdf(value))

      if (highestPdf == currentRuns.length - 2) {
        consecutiveOutliers += 1

        if (consecutiveOutliers > ignoreOutlierAfterNormalMeasurementCount) {
          consecutiveAnomalies = 0
          consecutiveOutliers = 0
          currentRuns = normalRuns.update(value)

          writeState(value, mostLikelyIndex)
          return
        }
      }
      else {
        consecutiveOutliers = 0
      }
    }
    else {
      consecutiveAnomalies = 0
      consecutiveOutliers = 0
    }

    // Save which run was most likely last time.
    previousMostLikelyIndex = mostLikelyIndex

    // If we've had too many 'abnormal' measurements in a row, we might need to
    // emit an event. We'll also reset to a fresh state, since most of the old
    // runs have a lot of data from before the changepoint.
    // TODO: Are there any runs that only have data from after the changepoint?
    // This would help us get regenerate our maxHistory a bit quicker.
    if (consecutiveAnomalies > changepointTriggerCount) {
      val newNormal = currentRuns.filter(_.dist.n == 1).head
      val severity = getSeverity(compositeOldNormal, newNormal)
      if (severity > severityThreshold) {
        newEvent(out, compositeOldNormal, newNormal, value, severity)
        reset(value)
      }
      consecutiveAnomalies = 0

      magicFlagOfGraphing = false
    }

    writeState(value, mostLikelyIndex)
    magicFlagOfGraphing = true
  }

  // ==== From here down is solely used for graphing
  def open(config: ParameterTool): Unit = {
    writer.write("NewEntry,")
    writer.write("PrevNormalMax,")
    writer.write("CurrentMaxI,")
    writer.write("NormalIsCurrent,")
    writer.write("ConsecutiveAnomalies,")
    writer.write("ConsecutiveOutliers,")
    (0 to maxHistory).foreach(i => writer.write(s"uid$i,prob$i,n$i,mean$i,var$i,"))
    writer.println()
    writer.flush()

    writerPdf.write("NewEntry,")
    (0 to maxHistory).foreach(i => writerPdf.write(s"pdf$i,"))
    writerPdf.println()
    writerPdf.flush()
  }

  private def writeState(value: MeasT, mostLikelyUid: Int): Unit = {
    val formatter: Run => String = x => s"${x.uid},${x.prob},${x.dist.n},${x.dist.mean},${x.dist.variance}"

    writer.print(s"${initialDistribution.asInstanceOf[NormalDistribution[MeasT]].mapFunction(value)},")
    writer.print(s"$previousMostLikelyIndex,")
    writer.print(s"$mostLikelyUid,")
    writer.print(s"$magicFlagOfGraphing,")
    writer.print(s"$consecutiveAnomalies,")
    writer.print(s"$consecutiveOutliers,")
    currentRuns.foreach(x => writer.print(s"${formatter(x)},"))
    writer.println()
    writer.flush()

    writerPdf.print(s"${initialDistribution.asInstanceOf[NormalDistribution[MeasT]].mapFunction(value)},")
    currentRuns.foreach(x => writerPdf.print(s"${x.dist.pdf(value)},"))
    writerPdf.println()
    writerPdf.flush()
  }

  private val getFile = "Normalise-Squash"
  private val writer = new PrintWriter(new File(s"../plot-ltsi/processed/$getFile.csv"))
  private val writerPdf = new PrintWriter(new File(s"../plot-ltsi/processed/pdf/$getFile-pdf.csv"))
}
