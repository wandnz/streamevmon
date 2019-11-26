package nz.net.wand.streamevmon.detectors.mode

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements._

import java.time.{Duration, Instant}

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo._
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.reflect._

class ModeDetector[MeasT <: Measurement: ClassTag] extends KeyedProcessFunction[Int, MeasT, Event] {

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
      new ValueStateDescriptor[mutable.Queue[(Int, MeasT)]](
        "History",
        TypeInformation.of(classOf[mutable.Queue[(Int, MeasT)]])
      )
    )

    modeIndexes = getRuntimeContext.getState(
      new ValueStateDescriptor[(Mode, Mode)](
        "Modes",
        TypeInformation.of(classOf[(Mode, Mode)])
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
  }

  private var lastObserved: ValueState[Instant] = _

  private var history: ValueState[mutable.Queue[(Int, MeasT)]] = _

  private case class Mode(value: Double, count: Int)
  private object Mode { def apply(input: (Double, Int)): Mode = Mode(input._1, input._2) }

  private var modeIndexes: ValueState[(Mode, Mode)] = _

  def reset(value: MeasT): Unit = {
    lastObserved.update(value.time)
    history.update(mutable.Queue())
    modeIndexes.update(Mode(-1, -1), Mode(-1, -1))
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
      case t: LatencyTSAmpICMP   => t.average
      case t: LatencyTSSmokeping => t.median.get
      case _ =>
        throw new IllegalArgumentException(
          s"Unsupported measurement type for Mode Detector: $value"
        )
    }
  }

  def updateModes(): Unit = {
    val groupedAndSorted =
      history.value.groupBy(x => mapFunction(x._2)).mapValues(_.size).toList.sortBy(_._2).reverse
    modeIndexes.update(
      (
        Mode(groupedAndSorted.head),
        Mode(groupedAndSorted.drop(1).head)
      ))
  }

  def newEvent(value: MeasT, out: Collector[Event]): Unit = {
    out.collect(
      new Event(
        "mode_events",
        value.stream,
        (modeIndexes.value._1.count.toDouble / maxHistory.toDouble).toInt,
        value.time,
        Duration.ZERO,
        s"New mode appeared! ${modeIndexes.value._1}",
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
      if (history.value.nonEmpty)
        history.value.maxBy(_._1)._1 + 1
      else 0
    }

    // Add the value into the queue.
    history.value.enqueue((getNewUid, value))
    if (history.value.length > maxHistory) {
      history.value.dequeue()
    }
    else {
      // If the queue isn't full yet, just give up.
      return
    }

    val oldModes = modeIndexes.value

    updateModes()

    def newModes: (Mode, Mode) = modeIndexes.value

    // If the primary mode hasn't changed, it's not an event.
    if (oldModes._1.value == newModes._1.value) {
      return
    }

    // If the primary mode isn't particularly outstanding, it's not an event.
    if (newModes._1.count < minFrequency) {
      return
    }

    // If the primary mode doesn't stick out a lot from the secondary mode,
    // it's not an event.
    if (newModes._1.count - newModes._2.count < minProminence) {
      return
    }

    // If we've made it all the way down here, it must be an event.
    newEvent(value, out)
  }
}
