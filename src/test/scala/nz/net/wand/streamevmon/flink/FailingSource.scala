package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.measurements.amp.ICMP
import nz.net.wand.streamevmon.SeedData

import java.time.Instant

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.windowing.time.Time

class FailingSource extends SourceFunction[ICMP] with CheckpointedFunction {
  def sendMeasurement(ctx: SourceFunction.SourceContext[ICMP], value: Int, time: Long): Unit = {
    val e = SeedData.icmp.expected
    ctx.collectWithTimestamp(
      ICMP(
        e.stream,
        e.loss,
        e.lossrate,
        e.median.map(_ => value * 1000),
        e.packet_size,
        e.results,
        e.rtts.map(_.map(_ => value * 1000)),
        Instant.ofEpochMilli(time)
      ),
      time
    )
  }

  def fail(): Unit = {
    throw new RuntimeException("Expected exception")
  }

  val initialTime: Long = 1000000000000L
  val numMeasurementsBeforeCheckpoint: Int = 100
  val checkpointTime: Long = initialTime + Time.seconds(1).toMilliseconds
  val timeBetweenMeasurements: Long = Time.seconds(1).toMilliseconds / numMeasurementsBeforeCheckpoint
  var currentTime: Long = initialTime
  var sentMeasurements: Int = 0

  @volatile var hasCheckpointed = false
  @volatile var hasRestored = false

  override def run(ctx: SourceFunction.SourceContext[ICMP]): Unit = {

    if (!hasCheckpointed || currentTime == initialTime) {
      println(s"Sending normal measurements")
      for (_ <- Range(0, numMeasurementsBeforeCheckpoint)) {
        sendMeasurement(
          ctx, 1, currentTime
        )
        currentTime += timeBetweenMeasurements
        sentMeasurements += 1
      }
    }
    else {
      println(s"Skipping normal measurements")
    }

    println(s"Waiting for checkpoint")
    while (!hasCheckpointed) {}

    if (!hasRestored) {
      Thread.sleep(1000)
      println(s"Failing")
      fail()
    }

    println(s"Sending abnormal measurements")
    for (_ <- Range(numMeasurementsBeforeCheckpoint, numMeasurementsBeforeCheckpoint * 2)) {
      sendMeasurement(
        ctx, 1000, currentTime
      )
      currentTime += 1
    }
  }

  override def cancel(): Unit = {}

  private var checkpointState: ListState[(Long, Int, Boolean)] = _

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    hasCheckpointed = true
    checkpointState.clear()
    checkpointState.add((currentTime, sentMeasurements, hasCheckpointed))
    println(s"Checkpointing: $checkpointState")
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    checkpointState = context
      .getOperatorStateStore
      .getListState(new ListStateDescriptor[(Long, Int, Boolean)]("FailingSource-state", classOf[(Long, Int, Boolean)]))

    if (context.isRestored) {
      val state = checkpointState.get().iterator().next()
      currentTime = state._1
      sentMeasurements = state._2
      hasCheckpointed = state._3
      hasRestored = true
    }
  }
}
