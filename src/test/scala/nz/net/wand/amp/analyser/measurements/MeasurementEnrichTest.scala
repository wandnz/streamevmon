package nz.net.wand.amp.analyser.measurements

import nz.net.wand.amp.analyser.SeedData
import nz.net.wand.amp.analyser.connectors.PostgresContainerSpec

class MeasurementEnrichTest extends PostgresContainerSpec {
  "Children of Measurement.enrich" should {
    "obtain the correct RichICMP object" in {
      assert(
        SeedData.icmp.expected.enrich() ===
        Some(SeedData.icmp.expectedRich)
      )
    }

    "obtain the correct RichDNS object" in {
      assert(
        SeedData.dns.expected.enrich() ===
        Some(SeedData.dns.expectedRich)
      )
    }

    "obtain the correct RichTraceroute object" in {
      assert(
        SeedData.traceroute.expected.enrich() ===
        Some(SeedData.traceroute.expectedRich)
      )
    }

    "obtain the correct RichTcpping object" in {
      assert(
        SeedData.tcpping.expected.enrich() ===
        Some(SeedData.tcpping.expectedRich)
      )
    }

    "obtain the correct RichHTTP object" in {
      assert(
        SeedData.http.expected.enrich() ===
        Some(SeedData.http.expectedRich)
      )
    }
  }
}
