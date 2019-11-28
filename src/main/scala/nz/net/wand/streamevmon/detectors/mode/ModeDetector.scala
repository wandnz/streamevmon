package nz.net.wand.streamevmon.detectors.mode

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements._
import nz.net.wand.streamevmon.Graphing

import java.awt.Color
import java.time.{Duration, Instant}

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo._
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.reflect._

class ModeDetector[MeasT <: Measurement : ClassTag]
  extends KeyedProcessFunction[Int, MeasT, Event]
          with Graphing {

  final val detectorName = "Mode Detector"

  private var maxHistory: Int = _

  private var minFrequency: Int = _

  private var minProminence: Int = _

  private var threshold: Double = _

  private var inactivityPurgeTime: Duration = _

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

    if (maxHistory < 5) {
      throw new IllegalArgumentException("maxHistory set too low! Must be at least 5.")
    }

    enablePlotting("/scratch/dwo4/graphs/mode.svg", detectorName)
    registerSeries("Mode Count", paint = Color.RED)
    registerSeries("Secondary Mode Count", paint = Color.BLUE)
  }

  override def close(): Unit = saveGraph()

  private var lastObserved: ValueState[Instant] = _

  private var history: ValueState[mutable.Queue[HistoryItem]] = _

  private case class Mode(value: Double, count: Int)
  private object Mode { def apply(input: (Double, Int)): Mode = Mode(input._1, input._2) }

  private case class HistoryItem(id: Int, value: MeasT)

  private case class ModeTuple(primary: Mode, secondary: Mode, lastEvent: Mode)

  private var modeIndexes: ValueState[ModeTuple] = _

  private val unsetModeIndexes: ModeTuple = ModeTuple(Mode(-1, -2), Mode(-3, -4), Mode(-5, -6))

  def reset(value: MeasT): Unit = {
    lastObserved.update(value.time)
    history.update(mutable.Queue())
    modeIndexes.update(unsetModeIndexes)
  }

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

  def mapFunction(value: MeasT): Double = {
    value match {
      case t: ICMP               => t.median.get
      case t: DNS                => t.rtt.get
      case t: TCPPing            => t.median.get
      case t: LatencyTSAmpICMP => t.average / 1000
      case t: LatencyTSSmokeping => t.median.get
      case _ =>
        throw new IllegalArgumentException(
          s"Unsupported measurement type for Mode Detector: $value"
        )
    }
  }

  def updateModes(): Unit = {
    val groupedAndSorted =
      history.value.groupBy(x => mapFunction(x.value)).mapValues(_.size).toList.sortBy(_._2).reverse

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

  def newEvent(value: MeasT, out: Collector[Event]): Unit = {
    out.collect(
      new Event(
        "mode_events",
        value.stream,
        (modeIndexes.value.primary.count.toDouble / maxHistory.toDouble).toInt,
        value.time,
        Duration.ZERO,
        s"Mode changed from ${modeIndexes.value.lastEvent.value} to ${modeIndexes.value.primary.value}!",
        Map()
      )
    )
  }

  override def processElement(
      value: MeasT,
      ctx: KeyedProcessFunction[Int, MeasT, Event]#Context,
      out: Collector[Event]
  ): Unit = {
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
      else 0
    }

    // Add the value into the queue.
    history.value.enqueue(HistoryItem(getNewUid, value))
    if (history.value.length > maxHistory) {
      history.value.dequeue()
    }

    updateModes()
    addToSeries("Mode Count", modeIndexes.value.primary.count, value.time)
    addToSeries("Secondary Mode Count", modeIndexes.value.secondary.count, value.time)

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
    // newModes.secondary will be (-1,-1) if there is only one value in history.
    // This is fine, since we shouldn't make it past the "has the mode changed"
    // barrier in this case. Even if we did, the mode would be very prominent.
    if (newModes.primary.count - newModes.secondary.count < minProminence) {
      return
    }

    // If we've made it all the way down here, it must be an event, unless this
    // seems to be the first standout mode. In that case, we don't know enough
    // about what happened in the past to talk about it.
    if (modeIndexes.value.lastEvent.value != unsetModeIndexes.lastEvent.value) {
      newEvent(value, out)
    }
    // We want to update the lastEvent mode record no matter what.
    modeIndexes.update(
      ModeTuple(
        modeIndexes.value.primary,
        modeIndexes.value.secondary,
        modeIndexes.value.primary
      )
    )
  }
}
