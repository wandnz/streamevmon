package nz.net.wand.streamevmon.checkpointing

import nz.net.wand.streamevmon.{Configuration, SeedData, TestBase}
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.amp.ICMP
import nz.net.wand.streamevmon.measurements.MeasurementKeySelector

import java.time.Instant

import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.streaming.api.operators.KeyedProcessOperator
import org.apache.flink.streaming.api.operators.co.CoProcessOperator
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.streaming.util.{KeyedOneInputStreamOperatorTestHarness, TwoInputStreamOperatorTestHarness}

trait CheckpointingTestBase extends TestBase {
  protected type KH = KeyedOneInputStreamOperatorTestHarness[String, ICMP, Event]

  protected def newHarness(implicit function: KeyedProcessFunction[String, ICMP, Event]): KH = {
    val h = new KH(
      new KeyedProcessOperator[String, ICMP, Event](function),
      new MeasurementKeySelector[ICMP],
      createTypeInformation[String]
    )
    h.getExecutionConfig.setGlobalJobParameters(Configuration.get(Array()))
    h
  }

  protected def newTwoInputHarness[IN1, IN2, OUT](implicit function: CoProcessFunction[IN1, IN2, OUT]): TwoInputStreamOperatorTestHarness[IN1, IN2, OUT] = {
    val h = new TwoInputStreamOperatorTestHarness(
      new CoProcessOperator[IN1, IN2, OUT](function)
    )
    h.getExecutionConfig.setGlobalJobParameters(Configuration.get(Array()))
    h
  }

  protected def snapshotAndRestart(harness: KH)(implicit function: KeyedProcessFunction[String, ICMP, Event]): KH = {
    lastGeneratedTime += 1
    checkpointId += 1
    val snapshot = harness.snapshot(checkpointId, lastGeneratedTime)
    harness.close()

    val nextHarness = newHarness(function)
    nextHarness.setup()
    nextHarness.initializeState(snapshot)
    nextHarness.open()
    nextHarness
  }

  protected def snapshotAndRestart[IN1, IN2, OUT](
    harness: TwoInputStreamOperatorTestHarness[IN1, IN2, OUT]
  )(
    implicit function: CoProcessFunction[IN1, IN2, OUT]
  ): TwoInputStreamOperatorTestHarness[IN1, IN2, OUT] = {
    lastGeneratedTime += 1
    checkpointId += 1
    val snapshot = harness.snapshot(checkpointId, lastGeneratedTime)
    harness.close()

    val nextHarness = newTwoInputHarness(function)
    nextHarness.setup()
    nextHarness.initializeState(snapshot)
    nextHarness.open()
    nextHarness
  }

  // Defaults to the nearest round number above Y2K, to give us some leeway
  // after the epoch for detectors which have a minimum event interval.
  protected var lastGeneratedTime: Long = 1000000000000L

  protected var checkpointId: Long = 1L

  protected def sendNormalMeasurement(harness: KH, times: Int): Unit = {
    val e = SeedData.icmp.expected
    for (_ <- Range(0, times)) {
      lastGeneratedTime += 1
      harness.processElement(
        ICMP(
          e.stream,
          e.loss,
          e.lossrate,
          e.median.map(_ * 1000),
          e.packet_size,
          e.results,
          e.rtts.map(_.map(_ * 1000)),
          Instant.ofEpochMilli(lastGeneratedTime)
        ),
        lastGeneratedTime
      )
    }
  }

  protected def sendAnomalousMeasurement(harness: KH, times: Int): Unit = {
    val e = SeedData.icmp.expected
    for (_ <- Range(0, times)) {
      lastGeneratedTime += 1
      harness.processElement(
        ICMP(
          e.stream,
          e.loss,
          e.lossrate,
          e.median.map(_ * 100000),
          e.packet_size,
          e.results,
          e.rtts.map(_.map(_ * 100000)),
          Instant.ofEpochMilli(lastGeneratedTime)
        ),
        lastGeneratedTime
      )
    }
  }

  protected def sendLossyMeasurement(harness: KH, times: Int): Unit = {
    val e = SeedData.icmp.expected
    for (_ <- Range(0, times)) {
      lastGeneratedTime += 1
      harness.processElement(
        ICMP(
          e.stream,
          Some(1),
          Some(1.0),
          None,
          e.packet_size,
          Some(1),
          Seq(None),
          Instant.ofEpochMilli(lastGeneratedTime)
        ),
        lastGeneratedTime
      )
    }
  }
}
