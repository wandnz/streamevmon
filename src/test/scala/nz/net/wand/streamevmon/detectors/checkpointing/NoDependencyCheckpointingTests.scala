package nz.net.wand.streamevmon.detectors.checkpointing

import nz.net.wand.streamevmon.detectors.changepoint.{ChangepointDetector, NormalDistribution}
import nz.net.wand.streamevmon.detectors.loss.LossDetector
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.detectors.MapFunction
import nz.net.wand.streamevmon.measurements.{ICMP, Measurement}

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala._

class NoDependencyCheckpointingTests extends CheckpointingTestBase {
  "Detectors with no external dependencies" should {
    "restore from checkpoints correctly" when {
      "type is ChangepointDetector" in {
        implicit val ti: TypeInformation[NormalDistribution[Measurement]] =
          TypeInformation.of(classOf[NormalDistribution[Measurement]])

        class IcmpToMedian() extends MapFunction[Measurement, Double] with Serializable {
          override def apply(t: Measurement): Double = t.asInstanceOf[ICMP].median.get
        }

        implicit val detector: ChangepointDetector[Measurement, NormalDistribution[Measurement]] =
          new ChangepointDetector[Measurement, NormalDistribution[Measurement]](
            new NormalDistribution[Measurement](mean = 0, new IcmpToMedian)
          )

        var harness = newHarness
        harness.open()

        sendNormalMeasurement(harness, times = 120)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness)

        sendAnomalousMeasurement(harness, times = 120)
        harness.getOutput shouldNot have size 0
      }

      "type is LossDetector" in {
        implicit val detector: LossDetector[Measurement] = new LossDetector[Measurement]
        var harness = newHarness
        harness.open()

        sendNormalMeasurement(harness, times = 30)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness)

        sendLossyMeasurement(harness, times = 30)
        harness.getOutput shouldNot have size 0
      }

      "type is ModeDetector" in {
        implicit val detector: ModeDetector[Measurement] = new ModeDetector[Measurement]
        var harness = newHarness
        harness.open()

        sendNormalMeasurement(harness, times = 120)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness)

        sendAnomalousMeasurement(harness, times = 120)
        harness.getOutput shouldNot have size 0
      }
    }
  }
}
