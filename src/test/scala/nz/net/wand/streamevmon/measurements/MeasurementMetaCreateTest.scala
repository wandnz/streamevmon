package nz.net.wand.streamevmon.measurements

import nz.net.wand.streamevmon.{PostgresContainerSpec, SeedData}
import nz.net.wand.streamevmon.measurements.amp._

class MeasurementMetaCreateTest extends PostgresContainerSpec {
  "PostgresConnection" should {
    lazy val pg = getPostgres

    "obtain correct ICMPMeta" in {
      pg.getMeta(SeedData.icmp.expected) shouldBe Some(SeedData.icmp.expectedMeta)
    }

    "obtain correct DNSMeta" in {
      pg.getMeta(SeedData.dns.expected) shouldBe Some(SeedData.dns.expectedMeta)
    }

    "obtain correct TracerouteMeta" in {
      pg.getMeta(SeedData.traceroute.expected) shouldBe Some(SeedData.traceroute.expectedMeta)
    }

    "obtain correct TCPPingMeta" in {
      pg.getMeta(SeedData.tcpping.expected) shouldBe Some(SeedData.tcpping.expectedMeta)
    }

    "obtain correct HTTPMeta" in {
      pg.getMeta(SeedData.http.expected) shouldBe Some(SeedData.http.expectedMeta)
    }

    "obtain several correct Meta objects" in {
      Seq(
        pg.getMeta(SeedData.icmp.expected),
        pg.getMeta(SeedData.dns.expected),
        pg.getMeta(SeedData.traceroute.expected),
        pg.getMeta(SeedData.tcpping.expected),
        pg.getMeta(SeedData.http.expected)
      ).foreach {
        case Some(x) =>
          x match {
            case _: ICMPMeta => x shouldBe SeedData.icmp.expectedMeta
            case _: DNSMeta => x shouldBe SeedData.dns.expectedMeta
            case _: TracerouteMeta => x shouldBe SeedData.traceroute.expectedMeta
            case _: TCPPingMeta => x shouldBe SeedData.tcpping.expectedMeta
            case _: HTTPMeta => x shouldBe SeedData.http.expectedMeta
            case _                 => fail("Created a type we didn't recognise")
          }
        case None => fail("Failed to create an object")
      }
    }
  }
}
