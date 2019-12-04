package nz.net.wand.streamevmon.measurements

import nz.net.wand.streamevmon.{SeedData, TestBase}
import nz.net.wand.streamevmon.SeedData.latencyTs

class LatencyTSCreateTest extends TestBase {
  "AMP ICMP result" should {
    "convert into a LatencyTSAmpICMP object" in {
      LatencyTSAmpICMP.create(latencyTs.ampLine, SeedData.latencyTs.amp.stream) shouldBe SeedData.latencyTs.amp
    }
  }

  "Smokeping result" should {
    "convert into a LatencyTSSmokeping object" when {
      "no loss detected" in {
        LatencyTSSmokeping.create(
          SeedData.latencyTs.smokepingLineNoLoss,
          SeedData.latencyTs.smokepingNoLoss.stream
        ) shouldBe SeedData.latencyTs.smokepingNoLoss
      }

      "some loss detected" in {
        LatencyTSSmokeping.create(
          SeedData.latencyTs.smokepingLineSomeLoss,
          SeedData.latencyTs.smokepingSomeLoss.stream
        ) shouldBe SeedData.latencyTs.smokepingSomeLoss
      }

      "total loss detected" in {
        LatencyTSSmokeping.create(
          SeedData.latencyTs.smokepingLineAllLoss,
          SeedData.latencyTs.smokepingAllLoss.stream
        ) shouldBe SeedData.latencyTs.smokepingAllLoss
      }

      "no data is present" in {
        LatencyTSSmokeping.create(
          SeedData.latencyTs.smokepingLineNoEntry,
          SeedData.latencyTs.smokepingNoEntry.stream
        ) shouldBe SeedData.latencyTs.smokepingNoEntry
      }

      "loss value does not match number of results" in {
        LatencyTSSmokeping.create(
          SeedData.latencyTs.smokepingLineMismatchedLoss,
          SeedData.latencyTs.smokepingMismatchedLoss.stream
        ) shouldBe SeedData.latencyTs.smokepingMismatchedLoss
      }
    }
  }
}
