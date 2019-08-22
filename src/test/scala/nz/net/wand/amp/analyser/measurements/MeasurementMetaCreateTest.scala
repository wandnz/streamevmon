package nz.net.wand.amp.analyser.measurements

import nz.net.wand.amp.analyser._
import nz.net.wand.amp.analyser.connectors.{PostgresConnection, PostgresContainerSpec}

class MeasurementMetaCreateTest extends PostgresContainerSpec {
  "PostgresConnection" should {
    "obtain correct ICMPMeta" in {
      assert(
        PostgresConnection.getICMPMeta(SeedData.icmp.expected) ===
        Some(SeedData.icmp.expectedMeta)
      )
    }

    "obtain correct DNSMeta" in {
      assert(
        PostgresConnection.getDNSMeta(SeedData.dns.expected) ===
        Some(SeedData.dns.expectedMeta)
      )
    }

    "obtain correct TracerouteMeta" in {
      assert(
        PostgresConnection.getTracerouteMeta(SeedData.traceroute.expected) ===
        Some(SeedData.traceroute.expectedMeta)
      )
    }

    "obtain correct TCPPingMeta" in {
      assert(
        PostgresConnection.getTcppingMeta(SeedData.tcpping.expected) ===
        Some(SeedData.tcpping.expectedMeta)
      )
    }

    "obtain correct HTTPMeta" in {
      assert(
        PostgresConnection.getHttpMeta(SeedData.http.expected) ===
        Some(SeedData.http.expectedMeta)
      )
    }

    "obtain several correct Meta objects" in {
      Seq(
        PostgresConnection.getMeta(SeedData.icmp.expected),
        PostgresConnection.getMeta(SeedData.dns.expected),
        PostgresConnection.getMeta(SeedData.traceroute.expected),
        PostgresConnection.getMeta(SeedData.tcpping.expected),
        PostgresConnection.getMeta(SeedData.http.expected)
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
