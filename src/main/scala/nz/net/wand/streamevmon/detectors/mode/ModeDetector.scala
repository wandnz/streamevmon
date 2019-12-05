package nz.net.wand.streamevmon.detectors.mode

import nz.net.wand.streamevmon.detectors.mode.ModeDetector._
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements._
import nz.net.wand.streamevmon.Graphing

import java.awt.Color
import java.time.{Duration, Instant}

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo._
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

import scala.collection.mutable

/** This detector measures the mode value of recent measurements, and inspects
  * the value for significant changes over time.
  *
  * @tparam MeasT The type of measurement to analyse.
  */
class ModeDetector[MeasT <: Measurement]
  extends KeyedProcessFunction[Int, MeasT, Event]
          with Graphing
          with CheckpointedFunction {

  final val detectorName = "Mode Detector"

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

    val config =
      getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
    maxHistory = config.getInt("detector.mode.maxHistory")
    minFrequency = config.getInt("detector.mode.minFrequency")
    minProminence = config.getInt("detector.mode.minProminence")
    threshold = config.getDouble("detector.mode.threshold")
    inactivityPurgeTime = Duration.ofSeconds(config.getInt("detector.mode.inactivityPurgeTime"))

    // This only needs to be 2 or so, but we've got a bit of breathing room to
    // let us function properly.
    if (maxHistory < 5) {
      throw new IllegalArgumentException("maxHistory set too low! Must be at least 5.")
    }

    registerSeries("Mode Count", paint = Color.RED)
    registerSeries("Secondary Mode Count", paint = Color.BLUE)
  }

  /** Save a graph when we're done (if that function is enabled) */
  override def close(): Unit = saveGraph()

  /** Resets the state of the detector. Happens on the first measurement, and
    * any time there's been a reasonable amount of time between the last one and
    * a new one.
    */
  def reset(value: MeasT): Unit = {
    lastObserved.update(value.time)
    history.update(mutable.Queue())
    modeIndexes.update(unsetModeIndexes)
  }

  /** We ditch lossy values. */
  def isLossy(value: MeasT): Boolean = {
    value match {
      case t: ICMP               => t.loss > 0
      case t: DNS                => t.lossrate > 0.0
      case t: TCPPing            => t.loss > 0
      case t: LatencyTSAmpICMP   => t.lossrate > 0.0
      case t: LatencyTSSmokeping => t.loss > 0
      case _ =>
        throw new IllegalArgumentException(
          s"Unsupported measurement type for Mode Detector: $value"
        )
    }
  }

  /** Maps a supported measurement to a useful value. Downscales them in order
    * for our mode detector to bucket values instead of using the raw, jittery
    * mesaurements.
    *
    * The downscaling method is naive and hasn't been strongly tested. It will
    * probably fail on low-latency links by making values too small, but that
    * just means that no events will be emitted.
    */
  def mapFunction(value: MeasT): Int = {
    value match {
      case t: ICMP => t.median.get / 1000
      case t: DNS => t.rtt.get / 1000
      case t: TCPPing => t.median.get / 1000
      case t: LatencyTSAmpICMP => t.average / 1000
      case t: LatencyTSSmokeping => (t.median.get / 1000).toInt
      case _ =>
        throw new IllegalArgumentException(
          s"Unsupported measurement type for Mode Detector: $value"
        )
    }
  }

  /** Finds the modes of the new dataset. If there's only one unique value,
    * we'll put down a dummy secondary value. This shouldn't be an issue, since
    * if there's one unique value it's been a very outstanding mode for quite a
    * while, and the gates in processValue won't get near any that touch the
    * secondary mode. If the history list is empty, an exception will be thrown,
    * but it shouldn't happen since this function only gets called after a
    * measurement has been added.
    */
  def updateModes(): Unit = {
    val groupedAndSorted = history.value
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
  def newEvent(value: MeasT, out: Collector[Event]): Unit = {
    val old = modeIndexes.value.lastEvent.value
    val current = modeIndexes.value.primary.value

    val severity = Event.changeMagnitudeSeverity(old, current)

    out.collect(
      new Event(
        "mode_events",
        value.stream,
        severity,
        value.time,
        Duration.ZERO,
        s"Mode changed from ${modeIndexes.value.lastEvent.value} to ${modeIndexes.value.primary.value}!",
        Map()
      )
    )
  }

  /** Called once per measurement. Generates zero or one events. */
  override def processElement(
      value: MeasT,
      ctx: KeyedProcessFunction[Int, MeasT, Event]#Context,
      out: Collector[Event]
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

    if (isLossy(value)) {
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
    history.value.enqueue(HistoryItem(getNewUid, mapFunction(value)))
    if (history.value.length > maxHistory) {
      history.value.dequeue()
    }

    updateModes()
    addToSeries("Mode Count", modeIndexes.value.primary.count, value.time)
    addToSeries("Secondary Mode Count", modeIndexes.value.secondary.count, value.time)

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
    if (!history.value.exists(x => x.value == newModes.lastEvent.value)) {
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
// everywhere. Flink doesn't appear to recognise them as POJOs, so they have to
// be serialised via Kryo, the same as tuples.
private object ModeDetector {

  case class Mode(value: Int, count: Int)

  object Mode {
    def apply(input: (Int, Int)): Mode = Mode(input._1, input._2)
  }

  case class HistoryItem(id: Int, value: Int)

  case class ModeTuple(primary: Mode, secondary: Mode, lastEvent: Mode)

}
