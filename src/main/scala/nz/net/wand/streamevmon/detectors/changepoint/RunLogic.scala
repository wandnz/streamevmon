package nz.net.wand.streamevmon.detectors.changepoint

import nz.net.wand.streamevmon.measurements.Measurement

import java.time.Instant

import org.apache.flink.api.common.typeinfo.TypeInformation

/** This trait is simply used to separate the run processing logic from the main
  * algorithm logic, since otherwise the class is hard to read as it does a
  * lot of different things.
  *
  * @see [[ChangepointProcessor]]
  */
abstract class RunLogic[MeasT <: Measurement : TypeInformation, DistT <: Distribution[MeasT] : TypeInformation] {

  /** Controls the decay rate of the probabilities of old runs. A hazard closer
    * to 1.0 will tend to be more sensitive. The value selected generally
    * provides consistent, useful results. Allowing configuration would likely
    * only cause confusion.
    */
  private val hazard: Double = 1.0 / 200.0

  // A couple of configuration values and methods are passed upwards so we can
  // use them here.
  protected var maxHistory: Int

  protected def newRunFor(value: MeasT, probability: Double): Run

  import scala.language.implicitConversions

  implicit protected def DistToDistT(d: Distribution[MeasT]): DistT = d.asInstanceOf[DistT]

  /** We keep a number of runs that contain probability distributions for the
    * last several measurements. Each of these has a probability to be the most
    * likely run for any given measurement, and we track changepoints by
    * noticing differences in which run is most likely.
    */
  case class Run(
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

    /** Updates the distributions for all the runs with the data from the new
      * measurement, and adjusts the run probabilities accordingly. It also adds
      * a new run, which might get squashed later.
      *
      * This method does most of the heavy lifting of the algorithm.
      *
      * @param value The new measurement.
      */
    def applyNewMeasurement(value: MeasT): Seq[Run] = {
      var updatedRuns = s
      var remainingProbability = 0.0

      updatedRuns = s.zipWithIndex.map { r =>
        // This value ends out being (1 - sum(prob)) for all prob in updatedRuns.
        remainingProbability += r._1.dist.pdf(value) * r._1.prob * hazard

        // Each run's distribution gets updated to include the new value in the model,
        // and the probabilities are updated according to the paper.
        Run(
          r._1.dist.withPoint(value, r._2 + 1),
          r._1.dist.pdf(value) * r._1.prob * (1 - hazard),
          r._1.start
        )
      }

      // Slap the new run on the front. It gets the rest of the probability.
      newRunFor(value, remainingProbability) +: updatedRuns
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
          secondLastRun.dist,
          secondLastRun.prob + lastRun.prob,
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
        Run(s.head.dist, 1.0, s.head.start) +: s
          .map(x => Run(x.dist, 0.0, x.start))
          .drop(1)
      }
      else {
        s.map(x => Run(x.dist, x.prob / total, x.start))
      }
    }

    def update(value: MeasT): Seq[Run] = {
      s
        // Apply probability changes according to the value of the new measurement.
        .applyNewMeasurement(value)
        // Condense old runs to comply with the maxRuns setting.
        .squashOldRuns
        // Normalise the probabilities so they all add to 1.
        .normalise
    }
  }
}
