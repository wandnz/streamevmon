package nz.net.wand.amp.analyser.measurements

import nz.net.wand.amp.analyser.SeedData

import org.scalatest.WordSpec

class MeasurementFromSubscription extends WordSpec {
  "Children of Measurement.create" should {
    "convert an entry from a subscription into an ICMP object" in {
      ICMP.create(SeedData.icmpSubscriptionLine) match {
        case Some(x) => assert(x === SeedData.expectedICMP)
        case None    => fail()
      }
    }

    "convert an entry from a subscription into a DNS object" in {
      DNS.create(SeedData.dnsSubscriptionLine) match {
        case Some(x) => assert(x === SeedData.expectedDNS)
        case None    => fail()
      }
    }

    "convert an entry from a subscription into a Traceroute object" in {
      Traceroute.create(SeedData.tracerouteSubscriptionLine) match {
        case Some(x) => assert(x === SeedData.expectedTraceroute)
        case None    => fail()
      }
    }
  }

  "MeasurementFactory.createMeasurement" should {
    "convert several entries to their respective Measurement subclasses" in {
      Seq(
        MeasurementFactory.createMeasurement(SeedData.dnsSubscriptionLine),
        MeasurementFactory.createMeasurement(SeedData.icmpSubscriptionLine),
        MeasurementFactory.createMeasurement(SeedData.tracerouteSubscriptionLine)
      ).foreach {
        case Some(x) =>
          x match {
            case _: ICMP       => assert(x === SeedData.expectedICMP)
            case _: DNS        => assert(x === SeedData.expectedDNS)
            case _: Traceroute => assert(x === SeedData.expectedTraceroute)
            case _             => fail()
          }
        case None => fail()
      }
    }
  }
}
