package nz.net.wand.streamevmon.detectors.distdiff

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.Measurement

import java.time.Duration

import org.apache.commons.math3.exception.util.LocalizedFormats
import org.apache.commons.math3.exception.OutOfRangeException
import org.apache.commons.math3.stat.inference.ChiSquareTest
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

import scala.collection.mutable

class DistDiffDetector[MeasT <: Measurement : TypeInformation]
  extends KeyedProcessFunction[Int, MeasT, Event] {

  val detectorName = "Distribution Difference Detector"

  private var lastObserved: ValueState[MeasT] = _

  private var recents: ValueState[mutable.Queue[MeasT]] = _
  private var longRecents: ValueState[mutable.Queue[MeasT]] = _

  private var inEvent: ValueState[Boolean] = _

  private var inactivityPurgeTime: Duration = _

  private var recentsCount: Int = _

  private var similarityIndex: Double = _

  lazy val chiSquare: ChiSquareTest = new ChiSquareTest()

  override def open(parameters: Configuration): Unit = {
    lastObserved = getRuntimeContext.getState(
      new ValueStateDescriptor[MeasT](
        "Last Observed Measurement",
        createTypeInformation[MeasT]
      )
    )

    recents = getRuntimeContext.getState(
      new ValueStateDescriptor[mutable.Queue[MeasT]](
        "Recent Measurements",
        createTypeInformation[mutable.Queue[MeasT]]
      )
    )

    longRecents = getRuntimeContext.getState(
      new ValueStateDescriptor[mutable.Queue[MeasT]](
        "Lots of Recent Measurements",
        createTypeInformation[mutable.Queue[MeasT]]
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
    similarityIndex = config.getDouble(s"$prefix.similarityIndex")

    // Check code ripped from ChiSquareTest.
    if (similarityIndex <= 0 ||
        similarityIndex > 0.5) {
      throw new OutOfRangeException(LocalizedFormats.OUT_OF_BOUND_SIGNIFICANCE_LEVEL,
                                    similarityIndex,
                                    0,
                                    0.5)
    }
  }

  def newEvent(
      value: MeasT,
      diff: Double,
      out: Collector[Event]
  ): Unit = {
    out.collect(
      Event(
        "distdiff_events",
        value.stream,
        diff.toInt, // TODO: Need some kind of scaling here.
        value.time,
        Duration.between(value.time, value.time),
        "DistDiff event",
        Map()
      ))
  }

  def reset(value: MeasT): Unit = {
    lastObserved.update(value)
    recents.update(mutable.Queue(value))
    longRecents.update(mutable.Queue())
    inEvent.update(false)
  }

  def addHistory(value: MeasT): Unit = {
    recents.value.enqueue(value)
    if (recents.value.length > recentsCount) {
      longRecents.value.enqueue(recents.value.dequeue())
    }
    if (longRecents.value.length > recentsCount) {
      longRecents.value.dequeue()
    }
  }

  def distributionDifference(): Double = {
    val expected = longRecents.value.filter(_.defaultValue.isDefined).map(_.defaultValue.get.toLong).toArray
    val observed = recents.value.filter(_.defaultValue.isDefined).map(_.defaultValue.get.toLong).toArray

    val r = chiSquare.chiSquareDataSetsComparison(expected, observed)
    val s = chiSquare.chiSquareTestDataSetsComparison(expected, observed)
    s
  }

  override def processElement(
      value: MeasT,
      ctx: KeyedProcessFunction[Int, MeasT, Event]#Context,
      out: Collector[Event]
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

    addHistory(value)

    if (longRecents.value.length < recentsCount) {
      return
    }

    val diff = distributionDifference()
    if (!inEvent.value && diff > similarityIndex) {
      newEvent(value, diff, out)
      inEvent.update(true)
    }
    if (inEvent.value && diff < similarityIndex / 2) {
      inEvent.update(false)
    }
  }
}
