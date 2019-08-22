package nz.net.wand.amp.analyser.measurements

import nz.net.wand.amp.analyser.SeedData

import org.scalatest.WordSpec

class MeasurementCreateTest extends WordSpec {
  "Children of Measurement.create" should {
    "convert an entry from a subscription into an ICMP object" in {
      assert(
        ICMP.create(SeedData.icmp.subscriptionLine) ===
        Some(SeedData.icmp.expected)
      )
    }

    "convert an entry from a subscription into a DNS object" in {
      assert(
        DNS.create(SeedData.dns.subscriptionLine) ===
        Some(SeedData.dns.expected)
      )
    }

    "convert an entry from a subscription into a Traceroute object" in {
      assert(
        Traceroute.create(SeedData.traceroute.subscriptionLine) ===
          Some(SeedData.traceroute.expected)
      )
    }

    "convert an entry from a subscription into a TCPPing object" in {
      assert(
        TCPPing.create(SeedData.tcpping.subscriptionLine) ===
          Some(SeedData.tcpping.expected)
      )
    }

    "convert an entry from a subscription into an HTTP object" in {
      assert(
        HTTP.create(SeedData.http.subscriptionLine) ===
          Some(SeedData.http.expected)
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
            case _: ICMP       => assert(x === SeedData.icmp.expected)
            case _: DNS        => assert(x === SeedData.dns.expected)
            case _: Traceroute => assert(x === SeedData.traceroute.expected)
            case _: TCPPing    => assert(x === SeedData.tcpping.expected)
            case _: HTTP       => assert(x === SeedData.http.expected)
            case _             => fail("Created a type we didn't recognise")
          }
        case None => fail("Failed to create an object")
      }
    }
  }
}
