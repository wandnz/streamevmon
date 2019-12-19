package nz.net.wand.streamevmon.detectors.checkpointing

import nz.net.wand.streamevmon.{Configuration, SeedData, TestBase}
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.MeasurementKeySelector
import nz.net.wand.streamevmon.measurements.{ICMP, Measurement}

import java.time.Instant

import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.operators.KeyedProcessOperator
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness

trait CheckpointingTestBase extends TestBase {
  protected type H = KeyedOneInputStreamOperatorTestHarness[String, Measurement, Event]

  protected def newHarness(implicit function: KeyedProcessFunction[String, Measurement, Event]): H = {
    val h = new H(
      new KeyedProcessOperator[String, Measurement, Event](function),
      new MeasurementKeySelector[Measurement],
      createTypeInformation[String]
    )
    h.getExecutionConfig.setGlobalJobParameters(Configuration.get(Array()))
    h
  }

  protected def snapshotAndRestart(harness: H)(implicit function: KeyedProcessFunction[String, Measurement, Event]): H = {
    lastGeneratedTime += 1
    val snapshot = harness.snapshot(1, lastGeneratedTime)
    harness.close()

    val nextHarness = newHarness(function)
    nextHarness.setup()
    nextHarness.initializeState(snapshot)
    nextHarness.open()
    nextHarness
  }

  // Defaults to the nearest round number above Y2K, to give us some leeway
  // after the epoch for detectors which have a minimum event interval.
  protected var lastGeneratedTime: Long = 1000000000000L

  protected def sendNormalMeasurement(harness: H, times: Int): Unit = {
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

  protected def sendAnomalousMeasurement(harness: H, times: Int): Unit = {
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

  protected def sendLossyMeasurement(harness: H, times: Int): Unit = {
    val e = SeedData.icmp.expected
    for (_ <- Range(0, times)) {
      lastGeneratedTime += 1
      harness.processElement(
        ICMP(
          e.stream,
          1,
          1,
          None,
          e.packet_size,
          1,
          Seq(None),
          Instant.ofEpochMilli(lastGeneratedTime)
        ),
        lastGeneratedTime
      )
    }
  }
}
