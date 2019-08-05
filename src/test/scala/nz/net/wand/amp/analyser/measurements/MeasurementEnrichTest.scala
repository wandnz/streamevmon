package nz.net.wand.amp.analyser.measurements

import nz.net.wand.amp.analyser.{PostgresContainerSpec, SeedData}

class MeasurementEnrichTest extends PostgresContainerSpec {
  "Children of Measurement.enrich" should {
    "obtain the correct RichICMP object" in {
      assertResult(
        Some(SeedData.icmp.expectedRich)
      )(
        SeedData.icmp.expected.enrich()
      )
    }

    "obtain the correct RichDNS object" in {
      assertResult(
        Some(SeedData.dns.expectedRich)
      )(
        SeedData.dns.expected.enrich()
      )
    }

    "obtain the correct RichTraceroute object" in {
      assertResult(
        Some(SeedData.traceroute.expectedRich)
      )(
        SeedData.traceroute.expected.enrich()
      )
    }

    "obtain the correct RichTcpping object" in {
      assertResult(
        Some(SeedData.tcpping.expectedRich)
      )(
        SeedData.tcpping.expected.enrich()
      )
    }

    "obtain the correct RichHTTP object" in {
      assertResult(
        Some(SeedData.http.expectedRich)
      )(
        SeedData.http.expected.enrich()
      )
    }
  }
}
