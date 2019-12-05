package nz.net.wand.streamevmon.detectors.mode

import nz.net.wand.streamevmon.{Configuration, SeedData, TestBase}
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.{ICMP, Measurement}

import java.time.Instant

import org.apache.flink.streaming.api.operators.KeyedProcessOperator
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness

class ModeStateTest extends TestBase {

  type H = KeyedOneInputStreamOperatorTestHarness[Int, Measurement, Event]

  private def newHarness: H = {
    val h = new H(
      new KeyedProcessOperator[Int, Measurement, Event](
        new ModeDetector[Measurement]
      ),
      (value: Measurement) => value.stream,
      createTypeInformation[Int]
    )
    h.getExecutionConfig.setGlobalJobParameters(Configuration.get(Array()))
    h
  }

  var lastGeneratedTime: Long = 0

  def sendNormalMeasurement(harness: H, times: Int = 1): Unit = {
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

  def sendAnomalousMeasurement(harness: H, times: Int = 1): Unit = {
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

  def snapshotAndRestart(harness: H): H = {
    lastGeneratedTime += 1
    val snapshot = harness.snapshot(1, lastGeneratedTime)
    harness.close()

    val nextHarness = newHarness
    nextHarness.setup()
    nextHarness.initializeState(snapshot)
    nextHarness.open()
    nextHarness
  }

  "ModeDetector" should {
    "not crash" in {
      var harness = newHarness
      harness.open()

      sendNormalMeasurement(harness, times = 120)
      harness.getOutput should have size 0

      harness = snapshotAndRestart(harness)

      sendAnomalousMeasurement(harness, times = 120)
      harness.getOutput shouldNot have size 0
      harness.close()
    }
  }
}
