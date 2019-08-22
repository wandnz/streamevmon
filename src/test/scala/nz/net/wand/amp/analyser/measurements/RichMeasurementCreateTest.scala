package nz.net.wand.amp.analyser.measurements

import nz.net.wand.amp.analyser.SeedData

import org.scalatest.WordSpec

class RichMeasurementCreateTest extends WordSpec {
  "Children of RichMeasurement.create" should {
    "merge an ICMP and ICMPMeta object" in {
      assert(
        RichICMP.create(SeedData.icmp.expected, SeedData.icmp.expectedMeta) ===
        Some(SeedData.icmp.expectedRich)
      )
    }

    "merge a DNS and DNSMeta object" in {
      assert(
        RichDNS.create(SeedData.dns.expected, SeedData.dns.expectedMeta) ===
        Some(SeedData.dns.expectedRich)
      )
    }

    "merge a Traceroute and TracerouteMeta object" in {
      assert(
        RichTraceroute.create(SeedData.traceroute.expected, SeedData.traceroute.expectedMeta) ===
        Some(SeedData.traceroute.expectedRich)
      )
    }

    "merge a TCPPing and TCPPingMeta object" in {
      assert(
        RichTCPPing.create(SeedData.tcpping.expected, SeedData.tcpping.expectedMeta) ===
        Some(SeedData.tcpping.expectedRich)
      )
    }

    "merge an HTTP and HTTPMeta object" in {
      assert(
        RichHTTP.create(SeedData.http.expected, SeedData.http.expectedMeta) ===
        Some(SeedData.http.expectedRich)
      )
    }
  }
}
