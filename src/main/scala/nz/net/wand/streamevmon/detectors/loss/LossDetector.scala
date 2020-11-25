package nz.net.wand.streamevmon.detectors.loss

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.traits.Measurement

import java.time.Duration

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.reflect._

/** Simple loss detector. See the package description for a detailed overview.
  *
  * @tparam MeasT The type of Measurement to ingest.
  */
class LossDetector[MeasT <: Measurement : ClassTag]
  extends KeyedProcessFunction[String, MeasT, Event]
          with CheckpointedFunction
          with HasFlinkConfig {

  final val flinkName = "Loss Detector"
  final val flinkUid = "loss-detector"
  final val configKeyGroup = "loss"

  /** The maximum number of measurements to retain. */
  private var maxHistory: Int = _

  /** The number of lossy measurements in the last maxHistory number of
    * measurements that must occur before an event is emitted.
    */
  private var lossCount: Int = _

  /** The number of consecutive lossy measurements before an event is emitted. */
  private var consecutiveCount: Int = _

  private var recentsStorage: ValueState[mutable.Queue[MeasT]] = _

  /** This gets set true when initializeState is called, and false when the
    * first measurement arrives. It help us avoid serialisation issues.
    */
  private var justInitialised = false

  /** Called once on instance creation. Sets up recentsStorage and gets config.
    *
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

    val config = configWithOverride(getRuntimeContext)
    maxHistory = config.getInt(s"detector.$configKeyGroup.maxHistory")
    lossCount = config.getInt(s"detector.$configKeyGroup.lossCount")
    consecutiveCount = config.getInt(s"detector.$configKeyGroup.consecutiveCount")
  }

  // Some helper functions here.
  private def recents: mutable.Queue[MeasT] = recentsStorage.value

  private def getConsecutiveLoss: Int = recents.reverse.takeWhile(_.isLossy).length

  private def getLossCount: Int = recents.count(_.isLossy)

  private def getOldestConsecutiveLoss: MeasT = recents.reverse.takeWhile(_.isLossy).last

  private def getOldestLoss: MeasT = recents.reverse.find(_.isLossy).get

  /** Called once per ingested measurement. Generates zero, one, or two events.
    *
    * @param value The new measurement.
    * @param ctx   The context of this function.
    * @param out   The collector to output events into.
    */
  override def processElement(
    value: MeasT,
    ctx  : KeyedProcessFunction[String, MeasT, Event]#Context,
    out  : Collector[Event]
  ): Unit = {
    // Setup if we need it.
    if (recentsStorage.value == null) {
      recentsStorage.update(new mutable.Queue[MeasT]())
    }

    // Get the lossiness before the current measurement happens. This could
    // be stateful, but it's easier to just recalculate it.
    val oldCount = getLossCount

    // Avoid checkpoint restoration issues. See ModeDetector for details.
    if (justInitialised) {
      recentsStorage.update(recentsStorage.value.map(identity))
      justInitialised = false
    }

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
          new Event(
            "loss_events",
            value.stream,
            (newConsecutive.toDouble / maxHistory.toDouble).toInt,
            value.time,
            Duration.between(oldest.time, value.time),
            s"Consecutive loss became worse! $newConsecutive in a row.",
            Map("type" -> "consecutive_loss")
          )
        )
      }
    }

    if (lossCount > 0) {
      if (newCount > oldCount) {
        if (newCount >= lossCount) {
          val oldest = getOldestLoss
          out.collect(
            new Event(
              "loss_events",
              value.stream,
              (newConsecutive.toDouble / maxHistory.toDouble).toInt,
              value.time,
              Duration.between(oldest.time, value.time),
              s"Loss ratio became worse! $oldCount/${recents.length} -> $newCount/${recents.length}",
              Map("type" -> "loss_ratio")
            )
          )
        }
      }
    }
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {}

  override def initializeState(context: FunctionInitializationContext): Unit = {
    justInitialised = true
  }
}
