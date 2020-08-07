package nz.net.wand.streamevmon.detectors.mode

import nz.net.wand.streamevmon.detectors.mode.ModeDetector._
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.{HasDefault, Measurement}

import java.math.{MathContext, RoundingMode}
import java.time.{Duration, Instant}

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo._
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

import scala.collection.mutable

/** This detector measures the mode value of recent measurements, and inspects
  * the value for significant changes over time. See the package object for
  * configuration details.
  *
  * @tparam MeasT The type of measurement to analyse.
  */
class ModeDetector[MeasT <: Measurement with HasDefault]
  extends KeyedProcessFunction[String, MeasT, Event]
          with CheckpointedFunction
          with HasFlinkConfig {

  final val flinkName = "Mode Detector"
  final val flinkUid = "mode-detector"
  final val configKeyGroup = "mode"

  /** The maximum number of measurements to retain. */
  private var maxHistory: Int = _

  /** The minimum number of times a value must occur in recent history to be
    * considered an event-worthy mode.
    */
  private var minFrequency: Int = _

  /** The primary mode must occur this many more times than the secondary mode
    * in recent history to be considered event-worthy.
    */
  private var minProminence: Int = _

  /** The minimum change amount threshold for a mode change to be event-worthy.
    * For example with the default value, a change from 12 to 13 is ignored,
    * but a change from 2 to 13 is significant.
    */
  private var threshold: Double = _

  /** The amount of time between measurements before history is forgotten. */
  private var inactivityPurgeTime: Duration = _

  /** The time of the last measurement we saw. Used along with inactivityPurgeTime. */
  private var lastObserved: ValueState[Instant] = _

  /** Our record of recent history. Maximum size is maxHistory. */
  private var history: ValueState[mutable.Queue[HistoryItem]] = _

  /** Keeps track of our primary and secondary modes, and the last event we had.
    * The secondary mode doesn't really need to be persistent, but the code is
    * a little nicer. The last event might be updated even if we don't emit an
    * event due to checks on change severity and how the change happened.
    */
  private var modeIndexes: ValueState[ModeTuple] = _

  /** A default value for modeIndexes so that we can tell if we've had an event
    * before.
    */
  private val unsetModeIndexes: ModeTuple = ModeTuple(Mode(-1, -2), Mode(-3, -4), Mode(-5, -6))

  /** This gets set true when initializeState is called, and false when the
    * first measurement arrives. It help us avoid serialisation issues.
    */
  private var justInitialised = false

  /** Called during initialisation. Sets up persistent state variables and
    * configuration.
    */
  override def open(parameters: Configuration): Unit = {
    lastObserved = getRuntimeContext.getState(
      new ValueStateDescriptor[Instant](
        "Last Observed Measurement Time",
        TypeInformation.of(classOf[Instant])
      )
    )

    history = getRuntimeContext.getState(
      new ValueStateDescriptor[mutable.Queue[HistoryItem]](
        "History",
        TypeInformation.of(classOf[mutable.Queue[HistoryItem]])
      )
    )

    modeIndexes = getRuntimeContext.getState(
      new ValueStateDescriptor[ModeTuple](
        "Modes",
        TypeInformation.of(classOf[ModeTuple])
      )
    )

    val config = configWithOverride(getRuntimeContext)
    maxHistory = config.getInt(s"detector.$configKeyGroup.maxHistory")
    minFrequency = config.getInt(s"detector.$configKeyGroup.minFrequency")
    minProminence = config.getInt(s"detector.$configKeyGroup.minProminence")
    threshold = config.getDouble(s"detector.$configKeyGroup.threshold")
    inactivityPurgeTime = Duration.ofSeconds(config.getInt(s"detector.$configKeyGroup.inactivityPurgeTime"))

    // This only needs to be 2 or so, but we've got a bit of breathing room to
    // let us function properly.
    if (maxHistory < 5) {
      throw new IllegalArgumentException("maxHistory set too low! Must be at least 5.")
    }
  }

  /** Resets the state of the detector. Happens on the first measurement, and
    * any time there's been a reasonable amount of time between the last one and
    * a new one.
    */
  def reset(value: MeasT): Unit = {
    lastObserved.update(value.time)
    history.update(mutable.Queue())
    modeIndexes.update(unsetModeIndexes)
  }

  private val roundingAmount: MathContext = new MathContext(2, RoundingMode.FLOOR)

  /** Buckets the input value in order to smooth out some of the natural jitter
    * in higher-magnitude datasets. We round to 2 significant figures, and take
    * the floor of the value. Flooring gives the same result as a division would
    * while retaining the magnitude of the inputs.
    */
  private def scale(value: Int): Int = {
    new java.math.BigDecimal(value).round(roundingAmount).intValue()
  }

  /** Finds the modes of the new dataset. If there's only one unique value,
    * we'll put down a dummy secondary value. This shouldn't be an issue, since
    * if there's one unique value it's been a very outstanding mode for quite a
    * while, and the gates in processValue won't get near any that touch the
    * secondary mode. If the history list is empty, an exception will be thrown,
    * but it shouldn't happen since this function only gets called after a
    * measurement has been added.
    */
  private def updateModes(): Unit = {
    val groupedAndSorted = history.value
      .map(x => HistoryItem(x.id, scale(x.value)))
      .groupBy(x => x.value)
      .mapValues(_.size)
      .toList
      .sortBy(_._2)
      .reverse

    if (groupedAndSorted.length > 1) {
      modeIndexes.update(
        ModeTuple(
          Mode(groupedAndSorted.head),
          Mode(groupedAndSorted.drop(1).head),
          modeIndexes.value.lastEvent
        ))
    }
    else if (groupedAndSorted.length == 1) {
      modeIndexes.update(
        ModeTuple(
          Mode(groupedAndSorted.head),
          Mode(-2, -2),
          modeIndexes.value.lastEvent
        )
      )
    }
    else {
      throw new IllegalStateException("Grouped recent values was empty!")
    }
  }

  /** Outputs a new event. The detection latency is 0. The severity
    * reflects the size of the change in mode values.
    */
  private def newEvent(value: MeasT, out: Collector[Event]): Unit = {
    val old = modeIndexes.value.lastEvent.value
    val current = modeIndexes.value.primary.value

    // To compensate for the 2 significant figure bucketing, we adjust the
    // values during the severity calculations to pretend that we only have
    // those two figures. For example, a pair of 12000 and 30000 is changed to
    // 12 and 30, which gives a much nicer severity calculation.

    // The magnitude is the amount we need to divide by to reach the target
    // value. log(0) is infinite, so we bypass that.
    def getMagnitude(i: Int): Int = {
      if (i == 0) {
        0
      }
      else {
        1 + Math.floor(Math.log10(i)).toInt
      }
    }

    // If the two magnitudes are different, we go for the smaller one to preserve
    // the significant figures. If they're the same, we subtract a little for
    // the same goal (otherwise 12000 and 30000 would become 1 and 3).
    val scaleFactor = {
      val oldM = getMagnitude(old)
      val currM = getMagnitude(current)
      if (oldM == currM) {
        oldM - 1
      }
      else {
        Math.min(oldM, currM)
      }
    }

    // Do the actual scaling, by just lopping off the right number of 0s.
    def divideByScale(i: Int): Int = i / Math.pow(10, scaleFactor - 1).toInt

    // And finally get the severity.
    val severity = Event.changeMagnitudeSeverity(divideByScale(old), divideByScale(current))

    out.collect(
      new Event(
        "mode_events",
        value.stream,
        severity,
        value.time,
        Duration.ZERO,
        s"Mode changed from $old to $current!",
        Map()
      )
    )
  }

  /** Called once per measurement. Generates zero or one events. */
  override def processElement(
    value: MeasT,
    ctx  : KeyedProcessFunction[String, MeasT, Event]#Context,
    out  : Collector[Event]
  ): Unit = {
    // If this is the first measurement or it's been too long since the last one,
    // we'll reset everything.
    if (lastObserved.value == null ||
      (!inactivityPurgeTime.isZero &&
        Duration
          .between(lastObserved.value, value.time)
          .compareTo(inactivityPurgeTime) > 0)) {
      reset(value)
      return
    }

    if (!Duration.between(lastObserved.value, value.time).isNegative) {
      lastObserved.update(value.time)
    }

    if (value.isLossy) {
      return
    }

    def getNewUid: Int = {
      if (history.value.nonEmpty) {
        history.value.maxBy(_.id).id + 1
      }
      else {
        0
      }
    }

    // There's a strange issue where the first item added to the queue after
    // restoring state from a checkpoint will instead result in the addition of
    // a 'super-null' value, which will throw a NoSuchElementException whenever
    // it is accessed via iteration rather than just returning null as usual.
    // This appears to stop that value from appearing, thus retaining the sanity
    // of the program. Of course, it's otherwise a waste of time and memory, so
    // we try to do it as little as possible.
    if (justInitialised) {
      history.update(history.value.map(identity))
      justInitialised = false
    }

    // Add the value into the queue.
    history.value.enqueue(HistoryItem(getNewUid, value.defaultValue.get.toInt))
    if (history.value.length > maxHistory) {
      history.value.dequeue()
    }

    updateModes()

    // If our history isn't full yet, there's no point checking for events.
    if (history.value.length < maxHistory) {
      return
    }

    def newModes: ModeTuple = modeIndexes.value

    // If the primary mode hasn't changed, it's not an event.
    if (newModes.lastEvent.value == newModes.primary.value) {
      return
    }

    // If the primary mode isn't particularly outstanding, it's not an event.
    if (newModes.primary.count < minFrequency) {
      return
    }

    // If the primary mode doesn't stick out a lot from the secondary mode,
    // it's not an event.
    // newModes.secondary.count will be <0 if there is only one value in history.
    // This is fine, since we shouldn't make it past the "has the mode changed"
    // barrier in this case.
    if (newModes.primary.count - newModes.secondary.count < minProminence) {
      return
    }

    // This will update our lastEvent record to the new mode. From here on,
    // a mode update has been successfully detected, but the gates will
    // consider whether it's important enough to notify about.
    def updateLastEvent(): Unit = {
      modeIndexes.update(
        ModeTuple(
          modeIndexes.value.primary,
          modeIndexes.value.secondary,
          modeIndexes.value.primary
        )
      )
    }

    // If the previous mode doesn't exist in the buffer anymore, it isn't really
    // fair to call this a mode change. Rather, we obviously haven't had a mode
    // for a long time and now we are settling into a constant pattern again.
    // It's not our job to report on changes in the overall time series pattern.
    if (!history.value.exists(x => scale(x.value) == newModes.lastEvent.value)) {
      updateLastEvent()
      return
    }

    // We'll steal some maths from the old version of this detector to generate
    // a reasonable importance threshold value.
    // The threshold ensures that there's a reasonable amount of change between
    // the old and new modes, or else we'll just send a bunch of events for
    // small changes or measurement noise. Using a logarithm lets us require a
    // larger relative change when modes are small.
    val thresh = {
      val calculatedThreshold = newModes.lastEvent.value / Math.log(newModes.lastEvent.value)
      if (calculatedThreshold < threshold) {
        threshold
      }
      else {
        calculatedThreshold
      }
    }
    if (Math.abs(newModes.lastEvent.value - newModes.primary.value) < thresh) {
      updateLastEvent()
      return
    }

    // If we've made it all the way down here, it must be an event, unless this
    // seems to be the first standout mode. In that case, we don't know enough
    // about what happened in the past to talk about it.
    if (newModes.lastEvent.value != unsetModeIndexes.lastEvent.value) {
      newEvent(value, out)
    }
    updateLastEvent()
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {}

  override def initializeState(context: FunctionInitializationContext): Unit = {
    // We don't care about manually storing state, so we just enable the flag
    // that lets us get around the fact that state restoration occasionally
    // breaks queues.
    justInitialised = true
  }
}

// These case classes just made the code look a bit nicer than using tuples
// everywhere. Kryo doesn't like to serialise them if they're inner classes
// of the detector class, so they're here instead.
private object ModeDetector {

  case class Mode(value: Int, count: Int)

  object Mode {
    def apply(input: (Int, Int)): Mode = Mode(input._1, input._2)
  }

  case class HistoryItem(id: Int, value: Int)

  case class ModeTuple(primary: Mode, secondary: Mode, lastEvent: Mode)

}
