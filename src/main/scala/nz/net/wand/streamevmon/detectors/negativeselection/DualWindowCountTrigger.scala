package nz.net.wand.streamevmon.detectors.negativeselection

import nz.net.wand.streamevmon.Logging

import org.apache.flink.api.common.functions.ReduceFunction
import org.apache.flink.api.common.state.ReducingStateDescriptor
import org.apache.flink.api.common.typeutils.base.LongSerializer
import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.streaming.api.windowing.triggers.{Trigger, TriggerResult}
import org.apache.flink.streaming.api.windowing.triggers.Trigger.TriggerContext
import org.apache.flink.streaming.api.windowing.windows.TimeWindow

import scala.reflect.ClassTag

/** A slightly modified version of Flink's CountTrigger, designed for the
  * [[TrainingDataSplitWindowAssigner]], which is used to split a dataset into
  * training and testing data in two separate windows.
  *
  * @param trigger1 The size of the first window.
  * @param trigger2 The size of the second window.
  * @tparam W1 The type of the first window.
  * @tparam W2 The type of the second window.
  * @tparam W  The parent type of both W1 and W2.
  * @tparam T  The type of the element which the windows contain.
  */
class DualWindowCountTrigger[T, W <: TimeWindow, W1 <: W : ClassTag, W2 <: W : ClassTag](
  trigger1: Long,
  trigger2: Long,
  onlyTriggerSecondWindowAfterFirst: Boolean = true
) extends Trigger[T, W] with Logging {

  private class Sum extends ReduceFunction[Long] {
    override def reduce(value1: Long, value2: Long): Long = value1 + value2
  }

  private val inputCountState = new ReducingStateDescriptor[Long](
    "count",
    new Sum(),
    LongSerializer.INSTANCE.asInstanceOf[TypeSerializer[Long]]
  )

  override def onElement(element: T, timestamp: Long, window: W, ctx: TriggerContext): TriggerResult = {

    val count = ctx.getPartitionedState(inputCountState)

    count.add(1L)

    window match {
      case _: W1 if count.get() >= trigger1 =>
        count.clear()
        TriggerResult.FIRE_AND_PURGE
      case _: W2 if count.get() >= trigger2 =>
        count.clear()
        TriggerResult.FIRE_AND_PURGE
      case _ =>
        TriggerResult.CONTINUE
    }
  }

  override def onProcessingTime(time: Long, window: W, ctx: TriggerContext): TriggerResult = {
    TriggerResult.CONTINUE
  }

  override def onEventTime(time: Long, window: W, ctx: TriggerContext): TriggerResult = {
    TriggerResult.CONTINUE
  }

  override def clear(window: W, ctx: TriggerContext): Unit = {
    ctx.getPartitionedState(inputCountState).clear()
  }

  override def canMerge: Boolean = true

  override def onMerge(window: W, ctx: Trigger.OnMergeContext): Unit = ctx.mergePartitionedState(inputCountState)

  override def toString: String = s"DualWindowCountTrigger($trigger1,$trigger2)"
}
