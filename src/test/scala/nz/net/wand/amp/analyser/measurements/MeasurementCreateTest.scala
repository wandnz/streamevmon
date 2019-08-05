package nz.net.wand.amp.analyser.measurements

import nz.net.wand.amp.analyser.SeedData

import org.scalatest.WordSpec

class MeasurementCreateTest extends WordSpec {
  "Children of Measurement.create" should {
    "convert an entry from a subscription into an ICMP object" in {
      assertResult(
        Some(SeedData.icmp.expected)
      )(
        ICMP.create(SeedData.icmp.subscriptionLine)
      )
    }

    "convert an entry from a subscription into a DNS object" in {
      assertResult(
        Some(SeedData.dns.expected)
      )(
        DNS.create(SeedData.dns.subscriptionLine)
      )
    }

    "convert an entry from a subscription into a Traceroute object" in {
      assertResult(
        Some(SeedData.traceroute.expected)
      )(
        Traceroute.create(SeedData.traceroute.subscriptionLine)
      )
    }

    "convert an entry from a subscription into a TCPPing object" in {
      assertResult(
        Some(SeedData.tcpping.expected)
      )(
        TCPPing.create(SeedData.tcpping.subscriptionLine)
      )
    }

    "convert an entry from a subscription into an HTTP object" in {
      assertResult(
        Some(SeedData.http.expected)
      )(
        HTTP.create(SeedData.http.subscriptionLine)
      )
    }
  }

  "MeasurementFactory.createMeasurement" should {
    "convert several entries to their respective Measurement subclasses" in {
      Seq(
        MeasurementFactory.createMeasurement(SeedData.icmp.subscriptionLine),
        MeasurementFactory.createMeasurement(SeedData.dns.subscriptionLine),
        MeasurementFactory.createMeasurement(SeedData.traceroute.subscriptionLine),
        MeasurementFactory.createMeasurement(SeedData.tcpping.subscriptionLine),
        MeasurementFactory.createMeasurement(SeedData.http.subscriptionLine)
      ).foreach {
        case Some(x) =>
          x match {
            case _: ICMP       => assertResult(SeedData.icmp.expected)(x)
            case _: DNS        => assertResult(SeedData.dns.expected)(x)
            case _: Traceroute => assertResult(SeedData.traceroute.expected)(x)
            case _: TCPPing    => assertResult(SeedData.tcpping.expected)(x)
            case _: HTTP       => assertResult(SeedData.http.expected)(x)
            case _             => fail("Created a type we didn't recognise")
          }
        case None => fail("Failed to create an object")
      }
    }
  }
}
