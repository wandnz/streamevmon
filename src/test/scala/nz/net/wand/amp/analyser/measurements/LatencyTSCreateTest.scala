package nz.net.wand.amp.analyser.measurements

import nz.net.wand.amp.analyser.SeedData

import org.scalatest.WordSpec

class LatencyTSCreateTest extends WordSpec {
  "AMP ICMP result" should {
    "convert into a LatencyTSAmpICMP object" in {
      assert(
        LatencyTSAmpICMP.create(SeedData.latencyTs.ampLine, SeedData.latencyTs.amp.stream) ===
          SeedData.latencyTs.amp
      )
    }
  }

  "Smokeping result" should {
    "convert into a LatencyTSSmokeping object" when {
      "no loss detected" in {
        assert(
          LatencyTSSmokeping.create(SeedData.latencyTs.smokepingLineNoLoss,
                                    SeedData.latencyTs.smokepingNoLoss.stream) ===
            SeedData.latencyTs.smokepingNoLoss
        )
      }

      "some loss detected" in {
        assert(
          LatencyTSSmokeping.create(SeedData.latencyTs.smokepingLineSomeLoss,
                                    SeedData.latencyTs.smokepingSomeLoss.stream) ===
            SeedData.latencyTs.smokepingSomeLoss
        )
      }

      "total loss detected" in {
        assert(
          LatencyTSSmokeping.create(SeedData.latencyTs.smokepingLineAllLoss,
                                    SeedData.latencyTs.smokepingAllLoss.stream) ===
            SeedData.latencyTs.smokepingAllLoss
        )
      }

      "no data is present" in {
        assert(
          LatencyTSSmokeping.create(SeedData.latencyTs.smokepingLineNoEntry,
                                    SeedData.latencyTs.smokepingNoEntry.stream) ===
            SeedData.latencyTs.smokepingNoEntry
        )
      }

      "loss value does not match number of results" in {
        assert(
          LatencyTSSmokeping.create(SeedData.latencyTs.smokepingLineMismatchedLoss,
                                    SeedData.latencyTs.smokepingMismatchedLoss.stream) ===
            SeedData.latencyTs.smokepingMismatchedLoss
        )
      }
    }
  }
}
