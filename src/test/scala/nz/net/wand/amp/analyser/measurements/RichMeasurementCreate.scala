package nz.net.wand.amp.analyser.measurements

import nz.net.wand.amp.analyser.SeedData

import org.scalatest.WordSpec

class RichMeasurementCreate extends WordSpec {
  "Children of RichMeasurement.create" should {
    "merge an ICMP and ICMPMeta object" in {
      val result =
        RichICMP
          .create(SeedData.expectedICMP, SeedData.expectedICMPMeta)
          .get

      assertResult(SeedData.expectedRichICMP)(result)
    }

    "merge a Traceroute and TracerouteMeta object" in {
      val result =
        RichTraceroute
          .create(SeedData.expectedTraceroute, SeedData.expectedTracerouteMeta)
          .get

      assertResult(SeedData.expectedRichTraceroute)(result)
    }

    "merge a DNS and DNSMeta object" in {
      val result =
        RichDNS
          .create(SeedData.expectedDNS, SeedData.expectedDNSMeta)
          .get

      assertResult(SeedData.expectedRichDNS)(result)
    }

    "merge a TCPPing and TCPPingMeta object" in {
      val result =
        RichTCPPing
          .create(SeedData.expectedTcpping, SeedData.expectedTcppingMeta)
          .get

      assertResult(SeedData.expectedRichTcpping)(result)
    }

    "merge an HTTP and HTTPMeta object" in {
      val result =
        RichHTTP
          .create(SeedData.expectedHTTP, SeedData.expectedHttpMeta)
          .get

      assertResult(SeedData.expectedRichHttp)(result)
    }
  }
}
