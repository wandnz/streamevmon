package nz.net.wand.streamevmon.detectors.distdiff

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.Logging

import org.apache.flink.api.common.state.ValueState

import scala.annotation.tailrec

/** Separates the tricky logic from the Flink stuff in the DistDiff detectors.
  */
trait DistDiffLogic extends Logging {

  /** Events are only emitted the first time they are detected, and not
    * continuously for long-running events.
    */
  protected var inEvent: ValueState[Boolean] = _

  /** `recents` and `longRecents` have at most this many values.
    * `times` has `recentsCount + 1` values, so we can keep track of the most
    * recent time in `longRecents`.
    */
  protected var recentsCount: Int = _

  /** Our calculated Z value must be greater than this threshold for an event
    * to be considered.
    */
  protected var zThreshold: Double = _

  /** The distributions must be this different for an event to be considered.
    * For example, a value of 1.05 means there must be at least a 5% change.
    */
  protected var minimumChange: Double = _

  /** When analysing the distributions, `recents` and `longRecents` are sorted,
    * then this many values are dropped from the high and low ends in order to
    * squash outliers.
    */
  protected var dropExtremeN: Int = _

  /** Compares the two distributions using a similar approach to the Kolmogorov-
    * Smirnov test. This method should be identical to the one used in netevmon.
    *
    * General approach:
    * - Compare the two smallest remaining values in the 2 dists (long and recent).
    * - If the long value is smaller, decrement the counter and remove the value
    * from the long dist.
    * - If the recent value is smaller, increment the counter and remove the value
    * from the recent dist.
    * - If they're equal, remove all instances of the equal value fromboth dists.
    * Increment for each value removed from recent, decrement for each value
    * removed from long.
    * - Continue until one of the distributions has no values left. For every
    * remaining value in the other dist, increment or decrement the counter
    * appropriately.
    * - The final difference is the counter when it was furthest away from zero
    * during this process.
    */
  protected def distributionDifference(
    longRecentsSorted: Seq[Double],
    recentsSorted    : Seq[Double]
  ): Double = {
    val windowLength = longRecentsSorted.length + recentsSorted.length
    val result = doDistDiff(longRecentsSorted, recentsSorted, windowLength)
    // Some scaling that was left in from the original K-S test. No real reason
    // to remove it, since the resulting number is pretty meaningless regardless.
    result * Math.sqrt(Math.pow(windowLength, 2) / (windowLength * 2))
  }

  /** This method does the heavy lifting for distributionDifference.
    *
    * @param old      The sorted subset of `longRecents` to use.
    * @param rec      The sorted subset of `recents` to use.
    * @param maxDepth The sum of the lengths of old and rec. We should never
    *                 recurse deeper than this.
    * @param depth    The current recursion depth.
    * @param rdiff    The current difference between distributions.
    * @param rdiffmax The maximum observed difference between distributions.
    */
  @tailrec
  private def doDistDiff(
    old: Seq[Double],
    rec: Seq[Double],
    maxDepth: Int,
    depth: Int = 0,
    rdiff: Double = 0.0,
    rdiffmax: Double = 0.0
  ): Double = {
    // If we've hit the end of one of the lists, we can use some maths magic to
    // determine the total offset that the rest of the other list would cause.
    if (old.isEmpty) {
      return {
        val newrdiff = rdiff + (-1.0 / recentsCount * (maxDepth - depth))
        if (Math.abs(newrdiff) > rdiffmax) {
          Math.abs(newrdiff)
        }
        else {
          rdiffmax
        }
      }
    }
    if (rec.isEmpty) {
      return {
        val newrdiff = rdiff + (1.0 / recentsCount * (maxDepth - depth))
        if (Math.abs(newrdiff) > rdiffmax) {
          Math.abs(newrdiff)
        }
        else {
          rdiffmax
        }
      }
    }

    // This shouldn't ever happen, since we consume one or both of the lists
    // at every iteration.
    if (depth >= maxDepth) {
      logger.error("DEPTH >= MAXDEPTH! This shouldn't happen!")
    }

    old.head.compareTo(rec.head) match {
      // Old less than recent
      case c if c < 0 =>
        val newrdiff = rdiff - (1.0 / recentsCount)
        val newrdiffmax = if (Math.abs(newrdiff) > rdiffmax) {
          newrdiff
        }
        else {
          rdiffmax
        }
        doDistDiff(old.drop(1), rec, maxDepth, depth + 1, newrdiff, newrdiffmax)
      // Old greater than recent
      case c if c > 0 =>
        val newrdiff = rdiff + (1.0 / recentsCount)
        val newrdiffmax = if (Math.abs(newrdiff) > rdiffmax) {
          newrdiff
        }
        else {
          rdiffmax
        }
        doDistDiff(old, rec.drop(1), maxDepth, depth + 1, newrdiff, newrdiffmax)
      // Old equal to recent
      // In this case, we skip all the equal values and look for one that's
      // different.
      case c if c == 0 =>
        doDistDiff(
          old.dropWhile(_ == old.head),
          rec.dropWhile(_ == rec.head),
          maxDepth,
          depth + 1,
          rdiff,
          rdiffmax
        )
    }
  }

  /** Determine the severity of a potential event. This method is lifted nearly
    * verbatim from netevmon as well, including the comments.
    *
    * @param old The sorted subset of `longRecents` used in `distributionDifference`.
    * @param rec The sorted subset of `recents` used in `distributionDifference`.
    * @param z   The value returned by `distributionDifference`.
    *
    * @return A severity from 0-100 if this is an event, or None if it isn't.
    */
  protected def eventSeverity(
    old: Seq[Double],
    rec: Seq[Double],
    z  : Double
  ): Option[Int] = {
    if (inEvent.value || z < zThreshold) {
      return None
    }

    val oldSum = old.sum
    val recSum = rec.sum
    val oldRange = old.max - old.min
    val recRange = rec.max - rec.min

    /* Change factor measures the difference in the sums of the samples
     * observed for each distribution. z only tells us whether the
     * distributions are different or not, but cannot distinguish between
     * a tiny change and a large change.
     *
     * We want to ignore cases where the distribution has only moved
     * subtly. To do this, we also try to ensure that the sum of the
     * observed values in each distribution are reasonably different.
     *
     * This will only work for some metrics (e.g. latency) where the
     * distribution generally won't change significantly and still have
     * the same sum.
     */
    val changeFactor = if (oldSum > recSum) {
      oldSum / recSum
    }
    else {
      recSum / oldSum
    }

    if (changeFactor < minimumChange || Math.abs(oldSum - recSum) < recentsCount) {
      return None
    }

    val oldMean: Double = oldSum / old.length
    val recMean: Double = recSum / rec.length

    if (recRange > 0.25 * oldRange && (Math.abs(oldMean - recMean) < 2 * oldRange)) {
      return None
    }

    // TODO: Constant scaling is probably not ideal.
    // We should test this detector with many different streams to see if it is
    // sufficient, or if we need something smarter like the ModeDetector does.
    Some(Event.changeMagnitudeSeverity(oldMean / 1000, recMean / 1000))
  }
}
