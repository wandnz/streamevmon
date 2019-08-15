package nz.net.wand.amp.analyser.measurements

import nz.net.wand.amp.analyser._
import nz.net.wand.amp.analyser.connectors.{PostgresConnection, PostgresContainerSpec}

class MeasurementMetaCreateTest extends PostgresContainerSpec {
  "PostgresConnection" should {
    "obtain correct ICMPMeta" in {
      assertResult(
        Some(SeedData.icmp.expectedMeta)
      )(
        PostgresConnection.getICMPMeta(SeedData.icmp.expected)
      )
    }

    "obtain correct DNSMeta" in {
      assertResult(
        Some(SeedData.dns.expectedMeta)
      )(
        PostgresConnection.getDNSMeta(SeedData.dns.expected)
      )
    }

    "obtain correct TracerouteMeta" in {
      assertResult(
        Some(SeedData.traceroute.expectedMeta)
      )(
        PostgresConnection.getTracerouteMeta(SeedData.traceroute.expected)
      )
    }

    "obtain correct TCPPingMeta" in {
      assertResult(
        Some(SeedData.tcpping.expectedMeta)
      )(
        PostgresConnection.getTcppingMeta(SeedData.tcpping.expected)
      )
    }

    "obtain correct HTTPMeta" in {
      assertResult(
        Some(SeedData.http.expectedMeta)
      )(
        PostgresConnection.getHttpMeta(SeedData.http.expected)
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
            case _: ICMPMeta       => assertResult(SeedData.icmp.expectedMeta)(x)
            case _: DNSMeta        => assertResult(SeedData.dns.expectedMeta)(x)
            case _: TracerouteMeta => assertResult(SeedData.traceroute.expectedMeta)(x)
            case _: TCPPingMeta    => assertResult(SeedData.tcpping.expectedMeta)(x)
            case _: HTTPMeta       => assertResult(SeedData.http.expectedMeta)(x)
            case _                 => fail("Created a type we didn't recognise")
          }
        case None => fail("Failed to create an object")
      }
    }
  }
}
