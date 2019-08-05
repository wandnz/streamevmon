package nz.net.wand.amp.analyser.measurements

import nz.net.wand.amp.analyser.SeedData

import org.scalatest.WordSpec

class RichMeasurementCreateTest extends WordSpec {
  "Children of RichMeasurement.create" should {
    "merge an ICMP and ICMPMeta object" in {
      assertResult(
        Some(SeedData.icmp.expectedRich)
      )(
        RichICMP.create(SeedData.icmp.expected, SeedData.icmp.expectedMeta)
      )
    }

    "merge a DNS and DNSMeta object" in {
      assertResult(
        Some(SeedData.dns.expectedRich)
      )(
        RichDNS.create(SeedData.dns.expected, SeedData.dns.expectedMeta)
      )
    }

    "merge a Traceroute and TracerouteMeta object" in {
      assertResult(
        Some(SeedData.traceroute.expectedRich)
      )(
        RichTraceroute.create(SeedData.traceroute.expected, SeedData.traceroute.expectedMeta)
      )
    }

    "merge a TCPPing and TCPPingMeta object" in {
      assertResult(
        Some(SeedData.tcpping.expectedRich)
      )(
        RichTCPPing.create(SeedData.tcpping.expected, SeedData.tcpping.expectedMeta)
      )
    }

    "merge an HTTP and HTTPMeta object" in {
      assertResult(
        Some(SeedData.http.expectedRich)
      )(
        RichHTTP.create(SeedData.http.expected, SeedData.http.expectedMeta)
      )
    }
  }
}
