package nz.net.wand.streamevmon.measurements

import nz.net.wand.streamevmon.connectors.PostgresContainerSpec
import nz.net.wand.streamevmon.SeedData

class MeasurementMetaCreateTest extends PostgresContainerSpec {
  "PostgresConnection" should {
    lazy val pg = getPostgres

    "obtain correct ICMPMeta" in {
      assert(
        pg.getMeta(SeedData.icmp.expected) ===
          Some(SeedData.icmp.expectedMeta)
      )
    }

    "obtain correct DNSMeta" in {
      assert(
        pg.getMeta(SeedData.dns.expected) ===
          Some(SeedData.dns.expectedMeta)
      )
    }

    "obtain correct TracerouteMeta" in {
      assert(
        pg.getMeta(SeedData.traceroute.expected) ===
          Some(SeedData.traceroute.expectedMeta)
      )
    }

    "obtain correct TCPPingMeta" in {
      assert(
        pg.getMeta(SeedData.tcpping.expected) ===
          Some(SeedData.tcpping.expectedMeta)
      )
    }

    "obtain correct HTTPMeta" in {
      assert(
        pg.getMeta(SeedData.http.expected) ===
          Some(SeedData.http.expectedMeta)
      )
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
            case _: ICMPMeta       => assert(x === SeedData.icmp.expectedMeta)
            case _: DNSMeta        => assert(x === SeedData.dns.expectedMeta)
            case _: TracerouteMeta => assert(x === SeedData.traceroute.expectedMeta)
            case _: TCPPingMeta    => assert(x === SeedData.tcpping.expectedMeta)
            case _: HTTPMeta       => assert(x === SeedData.http.expectedMeta)
            case _                 => fail("Created a type we didn't recognise")
          }
        case None => fail("Failed to create an object")
      }
    }
  }
}
