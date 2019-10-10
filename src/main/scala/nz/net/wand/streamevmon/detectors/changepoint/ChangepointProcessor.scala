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
  }

  private def newRunFor(value: MeasT): Run = {
    Run(
      getNewRunIndex,
      initialDistribution.withPoint(value, 1),
      1.0,
      value.time
    )
  }

  private val hazard = 1.0 / 200.0

  /** The maximum number of runs to retain */
  private val maxHistory = 20

  /** The number of consecutive outliers that belong to the same run that must
    * occur before we believe that run is the new normal.
    */
  private val sameRunConsecutiveTriggerCount = 10

  private val inactivityPurgeTime = Duration.ofSeconds(60)

  private var runIndexCounter = -1
  private def getNewRunIndex: Int = {
    runIndexCounter += 1
    runIndexCounter
  }

  private var currentRuns: Seq[Run] = Seq()
  private var normalIsCurrent: Boolean = true

  /** The last measurement we observed */
  private var lastObserved: MeasT = _

  /** The number of data points in a row that don't fit the expected data */
  private var consecutiveAnomalies: Int = _

  private var previousMostLikelyIndex: Int = _

  private val fakeRun = Run(-1, initialDistribution, 0.0, Instant.EPOCH)

  def reset(firstItem: MeasT): Unit = {
    runIndexCounter = -1
    currentRuns = Seq()
    normalIsCurrent = true

    lastObserved = firstItem

    consecutiveAnomalies = 0
    previousMostLikelyIndex = 0
  }

  def differentEnough(oldNormal: Run, newNormal: Run): Boolean = true

  // TODO: Calculate times and detection latency correctly
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

    // Process the new value: Create a new run, adjust the current runs, and
    // update the probabilities of the runs depending on the new measurement.
    currentRuns = currentRuns.update(value)

    // If this happens, something is probably wrong with the squashing algorithm.
    if (currentRuns.isEmpty) {
      logger.error("There were no runs left after applying growth probabilities! Resetting detector.")
      reset(value)
      return
    }

    // Find the most likely run.
    // We discount the newly added run because its probability will be way
    // too high until it gets another iteration or two under its belt.
    val mostLikelyIndex = if (currentRuns.length > 1) {
      currentRuns.dropRight(1).zipWithIndex.maxBy(_._1.prob)._2
    }
    else {
      0
    }

    // If the most likely run has changed, then the measurement doesn't match
    // our current model of the recent 'normal' behaviour of the measurements.
    // We'll update a counter to see if this happens for a while.
    if (mostLikelyIndex != previousMostLikelyIndex) {
      consecutiveAnomalies += 1
    }
    else {
      consecutiveAnomalies = 0
    }

    // Save which run was most likely last time.
    previousMostLikelyIndex = mostLikelyIndex

    // If we've had too many 'abnormal' measurements in a row, we might need to
    // emit an event. We'll also reset our counter and tweak a couple of other
    // values.
    if (consecutiveAnomalies > sameRunConsecutiveTriggerCount) {
      val oldNormal = currentRuns(previousMostLikelyIndex)
      val newNormal = currentRuns(mostLikelyIndex)
      if (differentEnough(oldNormal, newNormal)) {
        newEvent(out, oldNormal, newNormal, value)
      }
      consecutiveAnomalies = 0

      normalIsCurrent = false //temp for graphing
    }

    writeState(value, mostLikelyIndex)
    normalIsCurrent = true //temp for graphing
  }

  // ==== From here down is solely used for graphing
  def open(config: ParameterTool): Unit = {
    writer.write("NewEntry,")
    writer.write("PrevNormalMax,")
    writer.write("CurrentMaxI,")
    writer.write("NormalIsCurrent,")
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
    writer.print(s"$normalIsCurrent,")
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
