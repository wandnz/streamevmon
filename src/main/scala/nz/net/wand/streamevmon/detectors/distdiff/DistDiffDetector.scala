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

import scala.collection.mutable

/** This detector measures the difference between the distributions of
  * two sets of measurements: those observed recently, and those observed
  * slightly less recently. If a significant change is noticed, an event
  * is emitted.
  *
  * @tparam MeasT The type of measurement to analyse.
  */
class DistDiffDetector[MeasT <: Measurement : TypeInformation]
  extends KeyedProcessFunction[String, MeasT, Event]
          with DistDiffLogic {

  val detectorName = "Distribution Difference Detector"
  final val detectorUid = "distdiff-detector"

  private var lastObserved: ValueState[MeasT] = _

  /** The values of the more recent measurements. */
  private var recents: ValueState[mutable.Queue[Double]] = _
  /** The values of the less recent measurements. */
  private var longRecents: ValueState[mutable.Queue[Double]] = _
  /** The timestamps attached to the recent measurements. */
  private var times: ValueState[mutable.Queue[Instant]] = _

  /** If this time passes without a new measurement, all data is dropped. */
  private var inactivityPurgeTime: Duration = _

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
    old     : Seq[Double],
    rec     : Seq[Double],
    severity: Int,
    out     : Collector[Event]
  ): Unit = {
    val oldMean = old.sum / old.size
    val recMean = rec.sum / rec.size
    out.collect(
      Event(
        "distdiff_events",
        value.stream,
        severity,
        value.time,
        Duration.between(times.value.head, value.time),
        s"Distribution of ${value.getClass.getSimpleName} has changed. " +
          s"Mean has ${
            if (oldMean < recMean) {
              "increased"
            }
            else {
              "decreased"
            }
          } from $oldMean to $recMean",
        Map(
          "windowed" -> "false"
        )
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
      newEvent(value, old, rec, severity.get, out)
      inEvent.update(true)
    }
    // If the difference between distributions gets low enough, then we're no
    // longer in an event.
    if (diff < zThreshold / 2) {
      inEvent.update(false)
    }
  }
}