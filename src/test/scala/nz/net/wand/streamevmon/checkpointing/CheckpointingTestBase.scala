package nz.net.wand.streamevmon.checkpointing

import nz.net.wand.streamevmon.{Configuration, SeedData, TestBase}
import nz.net.wand.streamevmon.measurements.amp.ICMP
import nz.net.wand.streamevmon.measurements.MeasurementKeySelector
import nz.net.wand.streamevmon.measurements.traits.Measurement

import java.time.Instant

import org.apache.flink.streaming.api.functions.{KeyedProcessFunction, ProcessFunction}
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.streaming.api.operators.{KeyedProcessOperator, ProcessOperator}
import org.apache.flink.streaming.api.operators.co.CoProcessOperator
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.util._

import scala.reflect.ClassTag

trait CheckpointingTestBase extends TestBase {

  protected def newHarness[I <: Measurement : ClassTag, O](
    function: ProcessFunction[I, O]
  ): OneInputStreamOperatorTestHarness[I, O] = {
    val h = new OneInputStreamOperatorTestHarness(
      new ProcessOperator(function)
    )
    h.getExecutionConfig.setGlobalJobParameters(Configuration.get())
    h
  }

  protected def newHarness[I <: Measurement : ClassTag, O](
    function: KeyedProcessFunction[String, I, O]
  ): KeyedOneInputStreamOperatorTestHarness[String, I, O] = {
    val h = new KeyedOneInputStreamOperatorTestHarness(
      new KeyedProcessOperator(function),
      new MeasurementKeySelector[I](),
      createTypeInformation[String]
    )
    h.getExecutionConfig.setGlobalJobParameters(Configuration.get())
    h
  }

  protected def newHarness[IN1, IN2, OUT](
    function: CoProcessFunction[IN1, IN2, OUT]
  ): TwoInputStreamOperatorTestHarness[IN1, IN2, OUT] = {
    val h = new TwoInputStreamOperatorTestHarness(
      new CoProcessOperator(function)
    )
    h.getExecutionConfig.setGlobalJobParameters(Configuration.get())
    h
  }

  protected def snapshotAndRestart[I <: Measurement : ClassTag, O](
    harness : OneInputStreamOperatorTestHarness[I, O],
    function: ProcessFunction[I, O]
  ): OneInputStreamOperatorTestHarness[I, O] = {
    snapshotAndRestart[O, OneInputStreamOperatorTestHarness[I, O]](harness, newHarness(function))
  }

  protected def snapshotAndRestart[I <: Measurement : ClassTag, O](
    harness : KeyedOneInputStreamOperatorTestHarness[String, I, O],
    function: KeyedProcessFunction[String, I, O]
  ): KeyedOneInputStreamOperatorTestHarness[String, I, O] = {
    snapshotAndRestart[O, KeyedOneInputStreamOperatorTestHarness[String, I, O]](harness, newHarness(function))
  }

  protected def snapshotAndRestart[IN1, IN2, OUT](
    harness : TwoInputStreamOperatorTestHarness[IN1, IN2, OUT],
    function: CoProcessFunction[IN1, IN2, OUT]
  ): TwoInputStreamOperatorTestHarness[IN1, IN2, OUT] = {
    snapshotAndRestart[OUT, TwoInputStreamOperatorTestHarness[IN1, IN2, OUT]](harness, newHarness(function))
  }

  protected def snapshotAndRestart[O, HarnessT <: AbstractStreamOperatorTestHarness[O]](
    harness: HarnessT,
    newHarness: HarnessT
  ) = {
    lastGeneratedTime += 1
    checkpointId += 1
    val snapshot = harness.snapshot(checkpointId, lastGeneratedTime)
    harness.close()

    newHarness.setup()
    newHarness.initializeState(snapshot)
    newHarness.open()
    newHarness
  }

  // Defaults to the nearest round number above Y2K, to give us some leeway
  // after the epoch for detectors which have a minimum event interval.
  protected var lastGeneratedTime: Long = 1000000000000L

  protected var checkpointId: Long = 1L

  protected def sendNormalMeasurement[O](
    harness: KeyedOneInputStreamOperatorTestHarness[String, ICMP, O],
    times                                    : Int
  ): Unit = {
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

  protected def sendAnomalousMeasurement[O](
    harness: KeyedOneInputStreamOperatorTestHarness[String, ICMP, O],
    times                                       : Int
  ): Unit = {
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

  protected def sendLossyMeasurement[O](
    harness                                 : KeyedOneInputStreamOperatorTestHarness[String, ICMP, O],
    times                                   : Int
  ): Unit = {
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
