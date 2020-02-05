package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.measurements.amp.ICMP
import nz.net.wand.streamevmon.SeedData

import java.time.Instant
import java.util

import org.apache.flink.streaming.api.checkpoint.ListCheckpointed
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.windowing.time.Time

class FailingSource extends SourceFunction[Measurement] with ListCheckpointed[(Long, Int, Boolean)] {
  def sendMeasurement(ctx: SourceFunction.SourceContext[Measurement], value: Int, time: Long): Unit = {
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

  override def run(ctx: SourceFunction.SourceContext[Measurement]): Unit = {

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

  override def snapshotState(checkpointId: Long, timestamp: Long): util.List[(Long, Int, Boolean)] = {
    hasCheckpointed = true
    val r = new util.ArrayList[(Long, Int, Boolean)]()
    r.add((currentTime, sentMeasurements, hasCheckpointed))
    println(s"Checkpointing: $r")
    r
  }

  override def restoreState(state: util.List[(Long, Int, Boolean)]): Unit = {
    println(s"Restoring: $state")
    currentTime = state.get(0)._1
    sentMeasurements = state.get(0)._2
    hasCheckpointed = state.get(0)._3
    hasRestored = true
  }
}
