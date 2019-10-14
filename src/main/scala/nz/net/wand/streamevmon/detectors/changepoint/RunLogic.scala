package nz.net.wand.streamevmon.detectors.changepoint

import nz.net.wand.streamevmon.measurements.Measurement

import java.time.Instant

/** This trait is simply used to separate the run processing logic from the main
  * algorithm logic, since otherwise the class is very large and does too many
  * things.
  *
  * @see [[ChangepointProcessor]]
  */
private[changepoint] trait RunLogic[MeasT <: Measurement, DistT <: Distribution[MeasT]] {

  protected val hazard: Double
  protected val maxHistory: Int
  protected def newRunFor(value: MeasT): Run

  private[changepoint] case class Run(
      uid: Int,
      dist: DistT,
      prob: Double,
      start: Instant
  )

  implicit protected class SeqOfRuns(s: Seq[Run]) {

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

        current_runs = current_runs.updated(
          r + 1,
          Run(
            current_runs(r + 1).uid,
            current_runs(r).dist.withPoint(value, r + 1).asInstanceOf[DistT],
            current_runs(r).dist.pdf(value) * current_runs(r).prob * (1 - hazard),
            current_runs(r + 1).start
          )
        )
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
        Run(s.head.uid, s.head.dist, 1.0, s.head.start) +: s
          .map(
            x =>
              Run(
                x.uid,
                x.dist,
                0.0,
                x.start
            ))
          .drop(1)
      }
      else {
        s.map(
          x =>
            Run(
              x.uid,
              x.dist,
              x.prob / total,
              x.start
          ))
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
}
