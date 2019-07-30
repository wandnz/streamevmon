package nz.net.wand.amp.analyser.measurements

import nz.net.wand.amp.analyser.SeedData

import org.scalatest.FlatSpec

class RichMeasurementCreate extends FlatSpec {
  "RichICMP.create" should "merge an ICMP and ICMPMeta object" in {
    val result =
      RichICMP
        .create(SeedData.expectedICMP, SeedData.expectedICMPMeta)
        .get

    assertResult(SeedData.expectedRichICMP)(result)
  }

  "RichTraceroute.create" should "merge a Traceroute and TracerouteMeta object" in {
    val result =
      RichTraceroute
        .create(SeedData.expectedTraceroute, SeedData.expectedTracerouteMeta)
        .get

    assertResult(SeedData.expectedRichTraceroute)(result)
  }

  "RichDNS.create" should "merge a DNS and DNSMeta object" in {
    val result =
      RichDNS
        .create(SeedData.expectedDNS, SeedData.expectedDNSMeta)
        .get

    assertResult(SeedData.expectedRichDNS)(result)
  }
}
