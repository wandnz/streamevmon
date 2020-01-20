package nz.net.wand.streamevmon.detectors.distdiff

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.Measurement

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

class DistDiffDetector[MeasT <: Measurement : TypeInformation : ClassTag]
  extends KeyedProcessFunction[Int, MeasT, Event] {

  val detectorName = "Distribution Difference Detector"

  private var lastObserved: ValueState[MeasT] = _

  private var recents: ValueState[mutable.Queue[Double]] = _
  private var longRecents: ValueState[mutable.Queue[Double]] = _
  private var times: ValueState[mutable.Queue[Instant]] = _

  private var inEvent: ValueState[Boolean] = _

  private var inactivityPurgeTime: Duration = _

  private var recentsCount: Int = _

  private var zThreshold: Double = _

  private var minimumChange: Double = _

  private var dropExtremeN: Int = _

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

  def newEvent(
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

  /** This method reflects addHistory. */
  def addHistory(value: MeasT): Unit = {
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

  def distributionDifference(
    longRecentsSorted                         : mutable.Seq[Double],
    recentsSorted                             : mutable.Seq[Double]
  ): Double = {

    /* Compare two distributions using a similar approach to the
     * Kolmogorov-Smirnov test.
     * General approach:
     *  Compare the two smallest remaining values in the 2 dists (long
     *  and recent).
     *  If the long value is smaller, decrement the counter and remove
     *  the value from the long dist.
     *  If the recent value is smaller, increment the counter and remove
     *  the value from the recent dist.
     *  If they're equal, remove all instances of the equal value from
     *  both dists. Increment for each value removed from recent, decrement
     *  for each value removed from long.
     *  Continue until one of the distributions has no values left. For
     *  every remaining value in the other dist, increment or decrement
     *  the counter appropriately.
     *  The final difference is the counter when it was furthest away
     *  from zero during this process.
     */

    val windowLength = longRecentsSorted.length + recentsSorted.length
    val result = doDistDiff(longRecentsSorted, recentsSorted, windowLength)
    result * Math.sqrt(Math.pow(windowLength, 2) / (windowLength * 2))
  }

  @tailrec
  private def doDistDiff(
    old: mutable.Seq[Double],
    rec: mutable.Seq[Double],
    win: Int,
    depth: Int = 0,
    rdiff: Double = 0.0,
    rdiffmax: Double = 0.0
  ): Double = {
    if (old.isEmpty) {
      return {
        val newrdiff = rdiff + (-1.0 / recentsCount * (win - depth))
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
        val newrdiff = rdiff + (1.0 / recentsCount * (win - depth))
        if (Math.abs(newrdiff) > rdiffmax) {
          Math.abs(newrdiff)
        }
        else {
          rdiffmax
        }
      }
    }

    if (depth >= win) {
      println("DEPTH >= WIN! This shouldn't happen!")
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
        doDistDiff(old.drop(1), rec, win, depth + 1, newrdiff, newrdiffmax)
      // Old greater than recent
      case c if c > 0 =>
        val newrdiff = rdiff + (1.0 / recentsCount)
        val newrdiffmax = if (Math.abs(newrdiff) > rdiffmax) {
          newrdiff
        }
        else {
          rdiffmax
        }
        doDistDiff(old, rec.drop(1), win, depth + 1, newrdiff, newrdiffmax)
      // Old equal to recent
      // In this case, we skip all the equal values and look for one that's
      // different.
      case c if c == 0 =>
        doDistDiff(
          old.dropWhile(_ == old.head),
          rec.dropWhile(_ == rec.head),
          win,
          depth + 1,
          rdiff,
          rdiffmax
        )
    }
  }

  def eventSeverity(
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

    Some(Event.changeMagnitudeSeverity(oldMean / 1000, recMean / 1000))
  }

  def reset(value: MeasT): Unit = {
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

  /** This method reflects addValue. The last clause reflects compareDistributions. */
  override def processElement(
    value: MeasT,
    ctx  : KeyedProcessFunction[Int, MeasT, Event]#Context,
    out  : Collector[Event]
  ): Unit = {
    if (lastObserved.value == null ||
      Duration
        .between(lastObserved.value.time, value.time)
        .compareTo(inactivityPurgeTime) > 0) {
      reset(value)
      return
    }

    if (!Duration.between(lastObserved.value.time, value.time).isNegative) {
      lastObserved.update(value)
    }

    if (value.defaultValue.isEmpty) {
      return
    }

    addHistory(value)

    if (longRecents.value.length < recentsCount) {
      return
    }

    val old = longRecents.value.sorted.drop(dropExtremeN).dropRight(dropExtremeN)
    val rec = recents.value.sorted.drop(dropExtremeN).dropRight(dropExtremeN)

    val diff = distributionDifference(old, rec)

    val severity = eventSeverity(old, rec, diff)

    if (severity.isDefined) {
      newEvent(value, severity.get, out)
      inEvent.update(true)
    }
    if (diff < zThreshold / 2) {
      inEvent.update(false)
    }
  }
}
