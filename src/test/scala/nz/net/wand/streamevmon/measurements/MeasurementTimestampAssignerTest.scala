package nz.net.wand.streamevmon.measurements

import nz.net.wand.streamevmon.{SeedData, TestBase}

class MeasurementTimestampAssignerTest extends TestBase {
  "Correct timestamp should be assigned" in {
    val ts = new MeasurementTimestampAssigner

    val flattenedItems = Seq(
      SeedData.bigdata.flowsExpected,
      SeedData.esmond.expectedObjects.flatMap { objs =>
        Seq(objs.baseMeasurement, objs.baseRichMeasurement, objs.summaryRichMeasurement)
      }
    ).flatten

    (Seq(
      SeedData.icmp.expected,
      SeedData.icmp.lossyExpected,
      SeedData.icmp.expectedRich,
      SeedData.dns.expected,
      SeedData.dns.lossyExpected,
      SeedData.dns.expectedRich,
      SeedData.traceroute.expected,
      SeedData.traceroute.expectedRich,
      SeedData.traceroutePathlen.expected,
      SeedData.traceroutePathlen.expectedRich,
      SeedData.tcpping.expected,
      SeedData.tcpping.lossyExpected,
      SeedData.tcpping.expectedRich,
      SeedData.http.expected,
      SeedData.http.lossyExpected,
      SeedData.http.expectedRich,
      SeedData.latencyTs.amp,
      SeedData.latencyTs.smokepingNoLoss,
      SeedData.latencyTs.smokepingSomeLoss,
      SeedData.latencyTs.smokepingAllLoss,
      SeedData.latencyTs.smokepingNoEntry,
      SeedData.latencyTs.smokepingMismatchedLoss,
      SeedData.nab.expected
    ) ++ flattenedItems).foreach { meas =>
      ts.extractTimestamp(meas, 0L) shouldBe meas.time.toEpochMilli
    }
  }
}
