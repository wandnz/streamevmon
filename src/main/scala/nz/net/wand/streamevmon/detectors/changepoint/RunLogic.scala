package nz.net.wand.streamevmon.detectors.changepoint

import nz.net.wand.streamevmon.measurements.Measurement

import java.time.Instant

/** This trait is simply used to separate the run processing logic from the main
  * algorithm logic, since otherwise the class is hard to read since it does a
  * lot of different things.
  *
  * @see [[ChangepointProcessor]]
  */
private[changepoint] trait RunLogic[MeasT <: Measurement, DistT <: Distribution[MeasT]] {

  // A couple of configuration values and methods are passed upwards so we can
  // use them here.
  protected val hazard: Double
  protected val maxHistory: Int
  protected def newRunFor(value: MeasT): Run

  /** We keep a number of runs that contain probability distributions for the
    * last several measurements. Each of these has a probability to be the most
    * likely run for any given measurement, and we track changepoints by
    * noticing differences in which run is most likely.
    */
  private[changepoint] case class Run(
      uid: Int,
      dist: DistT,
      prob: Double,
      start: Instant
  )

  /** Extension method class for Seq[Run] which lets us use a more fluent API
    * in update(), and makes all the code read more nicely. Contains basically
    * all the operations on groups of runs that we care about.
    */
  implicit protected class SeqOfRuns(s: Seq[Run]) {

    def copy: Seq[Run] = s.map(identity)

    def addRun(newRun: Run): Seq[Run] = {
      s :+ newRun
    }

    /** Updates the distributions for all the runs with the data from the new
      * measurement, and adjusts the run probabilities accordingly. This is the
      * heavy math part of the algorithm.
      *
      * @param value  The new measurement.
      * @param newRun The new run that started with the new measurement.
      */
    def updateProbabilities(value: MeasT, newRun: Run): Seq[Run] = {
      var current_runs = s
      var current_weight = 0.0

      // We iterate over the array in reverse order, but skip the last one
      // because it's about to get overwritten.
      (current_runs.length - 2 to 0 by -1).foreach { r =>
        // This value ends out being (1 - sum(prob)) for all prob in current_runs.
        current_weight += current_runs(r).dist.pdf(value) * current_runs(r).prob * hazard

        // The (r+1)th run gets its distribution and probability overwritten
        // with an updated copy of the (r)th run's distribution+prob.
        current_runs = current_runs.updated(
          r + 1,
          Run(
            current_runs(r + 1).uid,
            current_runs(r).dist.withPoint(value, r + 1).asInstanceOf[DistT],
            current_runs(r).dist.pdf(value) * current_runs(r).prob * (1 - hazard),
            current_runs(r).start
          )
        )
      }

      // The new run gets the leftover probability.
      Run(
        newRun.uid,
        newRun.dist,
        current_weight,
        newRun.start
      ) +: current_runs.drop(1)
    }

    /** We comply with the maxHistory setting by squashing the oldest runs
      * together. We sum all the probabilities together, and keep the rest of
      * the data about only the youngest run after the length cut-off.
      */
    def squashOldRuns: Seq[Run] = {
      // Despite only one run being added per invocation of this function, we'll
      // keep on trimming off runs until we've hit the right length just in case.
      var newRuns = s
      while (newRuns.length > maxHistory) {
        val lastRun = newRuns.last
        val secondLastRun = newRuns.dropRight(1).last
        newRuns = newRuns.dropRight(2) :+ Run(
          secondLastRun.uid,
          secondLastRun.dist,
          lastRun.prob + secondLastRun.prob,
          secondLastRun.start
        )
      }
      newRuns
    }

    /** Make all the probabilities add to 1. This stops them growing stupidly
      * small while the most recent run still has a normally-sized probability.
      */
    def normalise: Seq[Run] = {
      val total = s.map(_.prob).sum

      // There's a special case where the probabilities have all gone to zero.
      // We'll just move all our probability to the newest run. This indicates
      // a potential changepoint.
      if (total == 0) {
        Run(s.head.uid, s.head.dist, 1.0, s.head.start) +: s
          .map(x => Run(x.uid, x.dist, 0.0, x.start))
          .drop(1)
      }
      else {
        s.map(x => Run(x.uid, x.dist, x.prob / total, x.start))
      }
    }

    def update(value: MeasT): Seq[Run] = {
      val newRun = newRunFor(value)

      // Add a new run created from the new measurement.
      s.addRun(newRun)
        // Apply probability changes according to the value of the new measurement.
        .updateProbabilities(value, newRun)
        // Condense old runs to comply with the maxRuns setting.
        .squashOldRuns
        // Normalise the probabilities so they all add to 1.
        .normalise
    }

    /** As the standard maxBy function, but we discount the rightmost value
      * since its probability is way too high.
      *
      * @return The index of the item returning the highest value when func is
      *         applied to it.
      */
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
