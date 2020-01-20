package nz.net.wand.streamevmon.detectors.distdiff

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.Logging

import java.time.{Duration, Instant}

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

import scala.annotation.tailrec
import scala.collection.mutable
import scala.reflect._

/** This detector measures the difference between the distributions of
  * two sets of measurements: those observed recently, and those observed
  * slightly less recently. If a significant change is noticed, an event
  * is emitted.
  *
  * @tparam MeasT The type of measurement to analyse.
  */
class DistDiffDetector[MeasT <: Measurement : TypeInformation : ClassTag]
  extends KeyedProcessFunction[String, MeasT, Event]
          with Logging {

  val detectorName = "Distribution Difference Detector"

  private var lastObserved: ValueState[MeasT] = _

  /** The values of the more recent measurements. */
  private var recents: ValueState[mutable.Queue[Double]] = _
  /** The values of the less recent measurements. */
  private var longRecents: ValueState[mutable.Queue[Double]] = _
  /** The timestamps attached to the recent measurements. */
  private var times: ValueState[mutable.Queue[Instant]] = _

  /** Events are only emitted the first time they are detected, and not
    * continuously for long-running events.
    */
  private var inEvent: ValueState[Boolean] = _

  /** If this time passes without a new measurement, all data is dropped. */
  private var inactivityPurgeTime: Duration = _

  /** `recents` and `longRecents` have at most this many values.
    * `times` has `recentsCount + 1` values, so we can keep track of the most
    * recent time in `longRecents`.
    */
  private var recentsCount: Int = _

  /** Our calculated Z value must be greater than this threshold for an event
    * to be considered.
    */
  private var zThreshold: Double = _

  /** The distributions must be this different for an event to be considered.
    * For example, a value of 1.05 means there must be at least a 5% change.
    */
  private var minimumChange: Double = _

  /** When analysing the distributions, `recents` and `longRecents` are sorted,
    * then this many values are dropped from the high and low ends in order to
    * squash outliers.
    */
  private var dropExtremeN: Int = _

  /** Called during initialisation. Sets up persistent state variables and
    * configuration.
    */
  override def open(parameters: Configuration): Unit = {
    lastObserved = getRuntimeContext.getState(
      new ValueStateDescriptor[MeasT](
        "Last Observed Measurement",
        createTypeInformation[MeasT]
      )
    )

    recents = getRuntimeContext.getState(
      new ValueStateDescriptor[mutable.Queue[Double]](
        "Recent Measurements",
        createTypeInformation[mutable.Queue[Double]]
      )
    )

    longRecents = getRuntimeContext.getState(
      new ValueStateDescriptor[mutable.Queue[Double]](
        "Less Recent Measurements",
        createTypeInformation[mutable.Queue[Double]]
      )
    )

    times = getRuntimeContext.getState(
      new ValueStateDescriptor[mutable.Queue[Instant]](
        "Times corresponding to recents then longRecents",
        createTypeInformation[mutable.Queue[Instant]]
      )
    )

    inEvent = getRuntimeContext.getState(
      new ValueStateDescriptor[Boolean](
        "Is an event happening?",
        createTypeInformation[Boolean]
      )
    )

    val config =
      getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
    val prefix = "detector.distdiff"
    inactivityPurgeTime = Duration.ofSeconds(config.getInt(s"$prefix.inactivityPurgeTime"))
    recentsCount = config.getInt(s"$prefix.recentsCount")
    zThreshold = config.getDouble(s"$prefix.zThreshold")
    dropExtremeN = config.getInt(s"$prefix.dropExtremeN")
    minimumChange = config.getDouble(s"$prefix.minimumChange")
  }

  /** Emits an event based on the provided severity and the stored distributions. */
  private def newEvent(
    value   : MeasT,
    severity: Int,
    out     : Collector[Event]
  ): Unit = {
    val oldMean = longRecents.value.sum / longRecents.value.length
    val recMean = recents.value.sum / recents.value.length
    out.collect(
      Event(
        "distdiff_events",
        value.stream,
        severity,
        value.time,
        Duration.between(times.value.head, value.time),
        s"Distribution of ${classTag[MeasT].runtimeClass.getSimpleName} has changed. " +
          s"Mean has ${
            if (oldMean < recMean) {
              "increased"
            }
            else {
              "decreased"
            }
          } from $oldMean to $recMean",
        Map()
      )
    )
  }

  /** Adds a value to the lists. The oldest value in `recents` is moved to the
    * start of `longRecents`, and the oldest value in `longRecents` is dropped.
    */
  private def addHistory(value: MeasT): Unit = {
    recents.value.enqueue(value.defaultValue.get)
    if (recents.value.length > recentsCount) {
      longRecents.value.enqueue(recents.value.dequeue())
    }
    if (longRecents.value.length > recentsCount) {
      longRecents.value.dequeue()
    }

    times.value.enqueue(value.time)
    if (times.value.length > recentsCount + 1) {
      times.value.dequeue()
    }
  }

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
  private def distributionDifference(
    longRecentsSorted                         : mutable.Seq[Double],
    recentsSorted                             : mutable.Seq[Double]
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
    old: mutable.Seq[Double],
    rec: mutable.Seq[Double],
    maxDepth: Int,
    depth   : Int = 0,
    rdiff   : Double = 0.0,
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
  private def eventSeverity(
    old: mutable.Seq[Double],
    rec: mutable.Seq[Double],
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

  /** Resets the detector back to its initial state.
    *
    * @param value The measurement to initialise with. Can be lossy.
    */
  private def reset(value: MeasT): Unit = {
    if (value.defaultValue.isEmpty) {
      lastObserved.update(null.asInstanceOf[MeasT])
    }
    else {
      lastObserved.update(value)
      recents.update(mutable.Queue(value.defaultValue.get))
      longRecents.update(mutable.Queue())
      times.update(mutable.Queue(value.time))
      inEvent.update(false)
    }
  }

  /** New measurements are ingested here. */
  override def processElement(
    value: MeasT,
    ctx  : KeyedProcessFunction[String, MeasT, Event]#Context,
    out  : Collector[Event]
  ): Unit = {
    // If there was no last value, or if it's been too long since the last
    // measurement, we reset.
    if (lastObserved.value == null ||
      Duration
        .between(lastObserved.value.time, value.time)
        .compareTo(inactivityPurgeTime) > 0) {
      reset(value)
      return
    }

    // If the last measurement was in the past compared to the new one, update
    // the last measurement.
    if (!Duration.between(lastObserved.value.time, value.time).isNegative) {
      lastObserved.update(value)
    }

    // If the value is lossy, we can't do anything with it.
    if (value.defaultValue.isEmpty) {
      return
    }

    // Otherwise, update the lists.
    addHistory(value)

    // If they're not full, we can't do anything more.
    if (longRecents.value.length < recentsCount) {
      return
    }

    // The algorithm needs the lists to be sorted with the outliers pruned, so
    // we do it here instead of calculating it multiple times.
    val old = longRecents.value.sorted.drop(dropExtremeN).dropRight(dropExtremeN)
    val rec = recents.value.sorted.drop(dropExtremeN).dropRight(dropExtremeN)

    // Get the 'z-value'...
    val diff = distributionDifference(old, rec)

    // ... and pass it to the severity calculator.
    val severity = eventSeverity(old, rec, diff)

    // If the severity is None, it wasn't an event.
    if (severity.isDefined) {
      newEvent(value, severity.get, out)
      inEvent.update(true)
    }
    // If the difference between distributions gets low enough, then we're no
    // longer in an event.
    if (diff < zThreshold / 2) {
      inEvent.update(false)
    }
  }
}
