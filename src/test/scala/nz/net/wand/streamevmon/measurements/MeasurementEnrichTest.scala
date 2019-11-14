package nz.net.wand.streamevmon.measurements

import nz.net.wand.streamevmon.{PostgresContainerSpec, SeedData}

class MeasurementEnrichTest extends PostgresContainerSpec {
  "Children of Measurement.enrich" should {

    lazy val pg = getPostgres

    "obtain the correct RichICMP object" in {
      assert(
        MeasurementFactory.enrichMeasurement(pg, SeedData.icmp.expected) ===
          Some(SeedData.icmp.expectedRich)
      )
    }

    "obtain the correct RichDNS object" in {
      assert(
        MeasurementFactory.enrichMeasurement(pg, SeedData.dns.expected) ===
          Some(SeedData.dns.expectedRich)
      )
    }

    "obtain the correct RichTraceroute object" in {
      assert(
        MeasurementFactory.enrichMeasurement(pg, SeedData.traceroute.expected) ===
          Some(SeedData.traceroute.expectedRich)
      )
    }

    "obtain the correct RichTcpping object" in {
      assert(
        MeasurementFactory.enrichMeasurement(pg, SeedData.tcpping.expected) ===
          Some(SeedData.tcpping.expectedRich)
      )
    }

    "obtain the correct RichHTTP object" in {
      assert(
        MeasurementFactory.enrichMeasurement(pg, SeedData.http.expected) ===
          Some(SeedData.http.expectedRich)
      )
    }
  }
}
