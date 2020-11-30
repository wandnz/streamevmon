package nz.net.wand.streamevmon.checkpointing

import nz.net.wand.streamevmon.{HarnessingTest, SeedData}
import nz.net.wand.streamevmon.detectors.baseline.BaselineDetector
import nz.net.wand.streamevmon.detectors.changepoint.{ChangepointDetector, NormalDistribution}
import nz.net.wand.streamevmon.detectors.distdiff.DistDiffDetector
import nz.net.wand.streamevmon.detectors.loss.LossDetector
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.detectors.spike.SpikeDetector
import nz.net.wand.streamevmon.events.grouping.graph.TraceroutePathGraph
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.amp.ICMP

import java.time.Instant

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness

class NoDependencyCheckpointingTests extends HarnessingTest {
  protected def sendNormalMeasurement[O](
    harness: KeyedOneInputStreamOperatorTestHarness[String, ICMP, O],
    times  : Int
  ): Unit = {
    val e = SeedData.icmp.expected
    for (_ <- Range(0, times)) {
      currentTime += 1
      harness.processElement(
        ICMP(
          e.stream,
          e.loss,
          e.lossrate,
          e.median.map(_ * 1000),
          e.packet_size,
          e.results,
          e.rtts.map(_.map(_ * 1000)),
          Instant.ofEpochMilli(currentTime)
        ),
        currentTime
      )
    }
  }

  protected def sendAnomalousMeasurement[O](
    harness: KeyedOneInputStreamOperatorTestHarness[String, ICMP, O],
    times  : Int
  ): Unit = {
    val e = SeedData.icmp.expected
    for (_ <- Range(0, times)) {
      currentTime += 1
      harness.processElement(
        ICMP(
          e.stream,
          e.loss,
          e.lossrate,
          e.median.map(_ * 100000),
          e.packet_size,
          e.results,
          e.rtts.map(_.map(_ * 100000)),
          Instant.ofEpochMilli(currentTime)
        ),
        currentTime
      )
    }
  }

  protected def sendLossyMeasurement[O](
    harness: KeyedOneInputStreamOperatorTestHarness[String, ICMP, O],
    times                                   : Int
  ): Unit = {
    val e = SeedData.icmp.expected
    for (_ <- Range(0, times)) {
      currentTime += 1
      harness.processElement(
        ICMP(
          e.stream,
          Some(1),
          Some(1.0),
          None,
          e.packet_size,
          Some(1),
          Seq(None),
          Instant.ofEpochMilli(currentTime)
        ),
        currentTime
      )
    }
  }

  "Detectors with no external dependencies" should {
    "restore from checkpoints correctly" when {
      "type is BaselineDetector" in {
        val detector: BaselineDetector[ICMP] = new BaselineDetector[ICMP]

        var harness = newHarness(detector)
        harness.open()

        sendNormalMeasurement(harness, times = 120)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness, detector)

        sendAnomalousMeasurement(harness, times = 120)
        harness.getOutput shouldNot have size 0
      }

      "type is ChangepointDetector" in {
        implicit val ti: TypeInformation[NormalDistribution[ICMP]] =
          TypeInformation.of(classOf[NormalDistribution[ICMP]])

        val detector: ChangepointDetector[ICMP, NormalDistribution[ICMP]] =
          new ChangepointDetector[ICMP, NormalDistribution[ICMP]](
            new NormalDistribution[ICMP](mean = 0)
          )

        var harness = newHarness(detector)
        harness.open()

        sendNormalMeasurement(harness, times = 120)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness, detector)

        sendAnomalousMeasurement(harness, times = 120)
        harness.getOutput shouldNot have size 0
      }

      "type is DistDiffDetector" in {
        val detector: DistDiffDetector[ICMP] = new DistDiffDetector[ICMP]

        var harness = newHarness(detector)
        harness.open()

        sendNormalMeasurement(harness, times = 120)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness, detector)

        sendAnomalousMeasurement(harness, times = 120)
        harness.getOutput shouldNot have size 0
      }

      "type is LossDetector" in {
        val detector: LossDetector[ICMP] = new LossDetector[ICMP]
        var harness = newHarness(detector)
        harness.open()

        sendNormalMeasurement(harness, times = 30)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness, detector)

        sendLossyMeasurement(harness, times = 30)
        harness.getOutput shouldNot have size 0
      }

      "type is ModeDetector" in {
        val detector: ModeDetector[ICMP] = new ModeDetector[ICMP]
        var harness = newHarness(detector)
        harness.open()

        sendNormalMeasurement(harness, times = 120)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness, detector)

        sendAnomalousMeasurement(harness, times = 120)
        harness.getOutput shouldNot have size 0
      }

      "type is SpikeDetector" in {
        val detector: SpikeDetector[ICMP] = new SpikeDetector[ICMP]
        var harness = newHarness(detector)
        harness.open()

        sendNormalMeasurement(harness, times = 100)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness, detector)

        sendAnomalousMeasurement(harness, times = 100)
        harness.getOutput shouldNot have size 0
      }
    }
  }

  "TraceroutePathGraph" should {
    "restore from checkpoints correctly" in {
      val graph = new TraceroutePathGraph[Event]()
      val path = SeedData.traceroute.expectedAsInetPath

      var harness = newHarness(graph)
      harness.open()

      currentTime += 1
      harness.processElement2(SeedData.traceroute.expectedAsInetPath, currentTime)

      // We can do this since there are no entries that need merging in the
      // example path.
      graph.mergedHosts should have size path.size
      graph.graph.vertexSet should have size path.size

      harness = snapshotAndRestart(harness, graph)

      graph.mergedHosts should have size path.size
      graph.graph.vertexSet should have size path.size
    }
  }
}
