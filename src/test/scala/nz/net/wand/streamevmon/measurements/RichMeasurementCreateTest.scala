package nz.net.wand.streamevmon.measurements

import nz.net.wand.streamevmon.{SeedData, TestBase}

class RichMeasurementCreateTest extends TestBase {
  "Children of RichMeasurement.create" should {
    "merge an ICMP and ICMPMeta object" in {
      RichICMP.create(SeedData.icmp.expected, SeedData.icmp.expectedMeta) shouldBe Some(SeedData.icmp.expectedRich)
    }

    "merge a DNS and DNSMeta object" in {
      RichDNS.create(SeedData.dns.expected, SeedData.dns.expectedMeta) shouldBe Some(SeedData.dns.expectedRich)
    }

    "merge a Traceroute and TracerouteMeta object" in {
      RichTraceroute.create(SeedData.traceroute.expected, SeedData.traceroute.expectedMeta) shouldBe Some(SeedData.traceroute.expectedRich)
    }

    "merge a TCPPing and TCPPingMeta object" in {
      RichTCPPing.create(SeedData.tcpping.expected, SeedData.tcpping.expectedMeta) shouldBe Some(SeedData.tcpping.expectedRich)
    }

    "merge an HTTP and HTTPMeta object" in {
      RichHTTP.create(SeedData.http.expected, SeedData.http.expectedMeta) shouldBe Some(SeedData.http.expectedRich)
    }
  }
}
