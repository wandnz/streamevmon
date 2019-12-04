package nz.net.wand.streamevmon.measurements

import nz.net.wand.streamevmon.{PostgresContainerSpec, SeedData}

class MeasurementEnrichTest extends PostgresContainerSpec {
  "Children of Measurement.enrich" should {

    lazy val pg = getPostgres

    "obtain the correct RichICMP object" in {
      MeasurementFactory.enrichMeasurement(pg, SeedData.icmp.expected) shouldBe Some(SeedData.icmp.expectedRich)
    }

    "obtain the correct RichDNS object" in {
      MeasurementFactory.enrichMeasurement(pg, SeedData.dns.expected) shouldBe Some(SeedData.dns.expectedRich)
    }

    "obtain the correct RichTraceroute object" in {
      MeasurementFactory.enrichMeasurement(pg, SeedData.traceroute.expected) shouldBe Some(SeedData.traceroute.expectedRich)
    }

    "obtain the correct RichTcpping object" in {
      MeasurementFactory.enrichMeasurement(pg, SeedData.tcpping.expected) shouldBe Some(SeedData.tcpping.expectedRich)
    }

    "obtain the correct RichHTTP object" in {
      MeasurementFactory.enrichMeasurement(pg, SeedData.http.expected) shouldBe Some(SeedData.http.expectedRich)
    }
  }
}
