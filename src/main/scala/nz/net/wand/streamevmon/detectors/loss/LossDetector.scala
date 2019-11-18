package nz.net.wand.streamevmon.detectors.loss

import nz.net.wand.streamevmon.events.LossEvent
import nz.net.wand.streamevmon.measurements._

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.reflect._

/** Simple loss detector. See the package description for a detailed overview.
  *
  * @tparam MeasT The type of Measurement to ingest.
  */
class LossDetector[MeasT <: Measurement: ClassTag]()
    extends KeyedProcessFunction[Int, MeasT, LossEvent] {

  /** The maximum number of measurements to retain. */
  var maxHistory: Int = _

  /** The number of lossy measurements in the last maxHistory number of
    * measurements that must occur before an event is emitted.
    */
  var lossCount: Int = _

  /** The number of consecutive lossy measurements before an event is emitted. */
  var consecutiveCount: Int = _

  final val detectorName = s"Loss Detector"

  private var recentsStorage: ValueState[mutable.Queue[MeasT]] = _

  /** Called once on instance creation. Sets up recentsStorage and gets config.
    * @param parameters Ignored.
    */
  override def open(parameters: Configuration): Unit = {
    recentsStorage = getRuntimeContext.getState(
      new ValueStateDescriptor[mutable.Queue[MeasT]](
        if (classTag[MeasT].runtimeClass == classOf[Measurement]) {
          "Measurement"
        }
        else {
          s"Measurement (${classTag[MeasT].runtimeClass.getSimpleName})"
        },
        TypeInformation.of(classOf[mutable.Queue[MeasT]])
      )
    )

    val config =
      getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
    maxHistory = config.getInt("detector.loss.maxHistory")
    lossCount = config.getInt("detector.loss.lossCount")
    consecutiveCount = config.getInt("detector.loss.consecutiveCount")
  }

  /** Whether or not the given measurement is lossy. A more future-proof approach
    * would be to accept a lambda argument to the class and use it as a map
    * function like the Changepoint detector does, but it's more work than is
    * really necessary. We can just expand this as we add more measurement types,
    * as we'll only ever care about one field from each type.
    */
  private def isLossy(t: MeasT): Boolean = {
    t match {
      case t: ICMP               => t.loss > 0
      case t: DNS                => t.lossrate > 0.0
      case _: HTTP               => false // Can't tell
      case t: TCPPing            => t.loss > 0
      case _: Traceroute         => false // Can't tell
      case t: LatencyTSAmpICMP   => t.lossrate > 0.0
      case t: LatencyTSSmokeping => t.loss > 0
      case _ =>
        throw new IllegalArgumentException(
          s"Unsupported measurement type for Loss Detector: $t"
        )
    }
  }

  // Some helper functions here.
  private def recents: mutable.Queue[MeasT] = recentsStorage.value

  private def getConsecutiveLoss: Int = recents.reverse.takeWhile(isLossy).length
  private def getLossCount: Int = recents.count(isLossy)

  private def getOldestConsecutiveLoss: MeasT = recents.reverse.takeWhile(isLossy).last
  private def getOldestLoss: MeasT = recents.reverse.find(isLossy).get

  /** Called once per ingested measurement. Generates zero, one, or two events.
    * @param value The new measurement.
    * @param ctx The context of this function.
    * @param out The collector to output events into.
    */
  override def processElement(
      value: MeasT,
      ctx: KeyedProcessFunction[Int, MeasT, LossEvent]#Context,
      out: Collector[LossEvent]
  ): Unit = {
    // Setup if we need it.
    if (recentsStorage.value == null) {
      recentsStorage.update(new mutable.Queue[MeasT]())
    }

    // Get the lossiness before the current measurement happens. This could
    // be stateful, but it's easier to just recalculate it.
    val oldCount = getLossCount

    // Update the queue.
    recents.enqueue(value)
    if (recents.length > maxHistory) {
      recents.dequeue()
    }

    // Check out how lossy we are now.
    val newConsecutive = getConsecutiveLoss
    val newCount = getLossCount

    // We generate an event for every new measurement that makes our state worse
    // than before, for both types of loss event. Each successive event has a
    // higher severity than the last, but there is no notification of the end of
    // a lossy period.
    if (consecutiveCount > 0) {
      if (newConsecutive >= consecutiveCount) {
        val oldest = getOldestConsecutiveLoss
        out.collect(
          LossEvent(
            Map("type" -> "consecutive_loss"),
            value.stream,
            (newConsecutive.toDouble / maxHistory.toDouble).toInt,
            value.time,
            s"Consecutive loss became worse! $newConsecutive in a row.",
            value.time.toEpochMilli - oldest.time.toEpochMilli
          )
        )
      }
    }

    if (lossCount > 0) {
      if (newCount > oldCount) {
        if (newCount >= lossCount) {
          val oldest = getOldestLoss
          out.collect(
            LossEvent(
              Map("type" -> "loss_ratio"),
              value.stream,
              (getLossCount.toDouble / maxHistory.toDouble).toInt,
              oldest.time,
              s"Loss ratio became worse! $oldCount/${recents.length} -> $newCount/${recents.length}",
              // The detection latency field here is a little meaningless, but that's fine.
              value.time.toEpochMilli - oldest.time.toEpochMilli
            ))
        }
      }
    }
  }
}
