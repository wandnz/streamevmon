package nz.net.wand.streamevmon.checkpointing

import nz.net.wand.streamevmon.detectors.baseline.BaselineDetector
import nz.net.wand.streamevmon.detectors.changepoint.{ChangepointDetector, NormalDistribution}
import nz.net.wand.streamevmon.detectors.distdiff.DistDiffDetector
import nz.net.wand.streamevmon.detectors.loss.LossDetector
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.detectors.spike.SpikeDetector
import nz.net.wand.streamevmon.measurements.amp.ICMP

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala._

class NoDependencyDetectorCheckpointingTests extends CheckpointingTestBase {
  "Detectors with no external dependencies" should {
    "restore from checkpoints correctly" when {
      "type is BaselineDetector" in {
        implicit val detector: BaselineDetector[ICMP] = new BaselineDetector[ICMP]

        var harness = newHarness
        harness.open()

        sendNormalMeasurement(harness, times = 120)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness)

        sendAnomalousMeasurement(harness, times = 120)
        harness.getOutput shouldNot have size 0
      }

      "type is ChangepointDetector" in {
        implicit val ti: TypeInformation[NormalDistribution[ICMP]] =
          TypeInformation.of(classOf[NormalDistribution[ICMP]])

        implicit val detector: ChangepointDetector[ICMP, NormalDistribution[ICMP]] =
          new ChangepointDetector[ICMP, NormalDistribution[ICMP]](
            new NormalDistribution[ICMP](mean = 0)
          )

        var harness = newHarness
        harness.open()

        sendNormalMeasurement(harness, times = 120)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness)

        sendAnomalousMeasurement(harness, times = 120)
        harness.getOutput shouldNot have size 0
      }

      "type is DistDiffDetector" in {
        implicit val detector: DistDiffDetector[ICMP] = new DistDiffDetector[ICMP]

        var harness = newHarness
        harness.open()

        sendNormalMeasurement(harness, times = 120)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness)

        sendAnomalousMeasurement(harness, times = 120)
        harness.getOutput shouldNot have size 0
      }

      "type is LossDetector" in {
        implicit val detector: LossDetector[ICMP] = new LossDetector[ICMP]
        var harness = newHarness
        harness.open()

        sendNormalMeasurement(harness, times = 30)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness)

        sendLossyMeasurement(harness, times = 30)
        harness.getOutput shouldNot have size 0
      }

      "type is ModeDetector" in {
        implicit val detector: ModeDetector[ICMP] = new ModeDetector[ICMP]
        var harness = newHarness
        harness.open()

        sendNormalMeasurement(harness, times = 120)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness)

        sendAnomalousMeasurement(harness, times = 120)
        harness.getOutput shouldNot have size 0
      }

      "type is SpikeDetector" in {
        implicit val detector: SpikeDetector[ICMP] = new SpikeDetector[ICMP]
        var harness = newHarness
        harness.open()

        sendNormalMeasurement(harness, times = 100)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness)

        sendAnomalousMeasurement(harness, times = 100)
        harness.getOutput shouldNot have size 0
      }
    }
  }
}
