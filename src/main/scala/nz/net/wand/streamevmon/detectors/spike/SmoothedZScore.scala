package nz.net.wand.streamevmon.detectors.spike

import org.apache.commons.math3.stat.descriptive.SummaryStatistics

import scala.collection.mutable

/** Smoothed zero-score alogrithm shamelessly copied from [[https://stackoverflow.com/a/22640362/6029703]]
  *
  * Scala implementation described here [[https://stackoverflow.com/a/48231877]]
  *
  * Working offline code from the linked gist at [[https://gist.github.com/ecopoesis/587602a39fddd7001d1ca54a16884417]]
  *
  * Online reimplementation by Daniel Oosterwijk for this project.
  *
  * Uses a rolling mean and a rolling deviation (separate) to identify peaks in a vector
  *
  * @param lag       - The lag of the moving window (i.e. how big the window is)
  * @param threshold - The z-score at which the algorithm signals (i.e. how many standard deviations away from the moving mean a peak (or signal) is)
  * @param influence - The influence (between 0 and 1) of new signals on the mean and standard deviation (how much a peak (or signal) should affect other values near it)
  *
  */
case class SmoothedZScore(
  lag: Int = 30,
  threshold: Double = 5d,
  influence: Double = 0d
) {

  var history: mutable.Queue[Double] = mutable.Queue()

  var lastMean: Double = Double.NaN
  var lastStd: Double = Double.NaN

  /** Checks if a new value is an unexpected peak.
    *
    * @param value The new input to analyze in context with the previous inputs.
    *
    * @return The signal associated with the new input.
    */
  def addValue(value: Double): SignalType.Value = {
    // We start off by determining if the new value is a signal or not, since
    // this only depends on the new value and the summary statistics of the
    // previous iteration.

    // If we haven't got enough values to work with yet, just return no signal.
    val haveEnoughValues = history.size >= lag
    // If the distance between the new value and the last average is enough
    // standard deviations (threshold many) away, then it's a signal.
    val distanceLargeEnough = Math.abs(value - lastMean) > threshold * lastStd

    val signal: SignalType.Value = if (haveEnoughValues && distanceLargeEnough) {
      // Filter any signals out from the history proportionally to the influence
      // parameter.
      history.enqueue((influence * value) + ((1 - influence) * history.last))

      // Find out if it's a positive or negative signal.
      if (value > lastMean) {
        SignalType.Positive
      }
      else {
        SignalType.Negative
      }
    }
      else {
      // If it's not a signal, we just add the value to the history normally.
      history.enqueue(value)
      SignalType.NoSignal
    }

    // If we have too many values, get rid of the oldest.
    if (history.size > lag) {
      history.dequeue()
    }

    // Calculate the summary statistics including the new value, so that we can
    // use the values from this iteration in the next iteration.
    val stats = new SummaryStatistics()
    history.foreach(stats.addValue)
    lastMean = stats.getMean
    // getStandardDeviation() uses sample variance (not what we want)
    lastStd = Math.sqrt(stats.getPopulationVariance)

    signal
  }

  /** Refreshes the queue to avoid issues with broken entries. */
  def refreshState(): Unit = {
    history = history.map(identity)
  }

  /** Resets this instance to its original state. */
  def reset(): Unit = {
    history = mutable.Queue()
  }
}
