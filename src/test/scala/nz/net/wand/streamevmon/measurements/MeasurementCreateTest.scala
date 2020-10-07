package nz.net.wand.streamevmon.measurements

import nz.net.wand.streamevmon.{SeedData, TestBase}
import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.bigdata.Flow

class MeasurementCreateTest extends TestBase {
  "Children of Measurement.create" should {
    "convert an entry from a subscription into an ICMP object" in {
      ICMP.create(SeedData.icmp.subscriptionLine) shouldBe Some(SeedData.icmp.expected)
    }

    "convert an entry from a subscription into a DNS object" in {
      DNS.create(SeedData.dns.subscriptionLine) shouldBe Some(SeedData.dns.expected)
    }

    "convert an entry from a subscription into a TraceroutePathlen object" in {
      TraceroutePathlen.create(SeedData.traceroutePathlen.subscriptionLine) shouldBe Some(SeedData.traceroutePathlen.expected)
    }

    "convert an entry from a subscription into a TCPPing object" in {
      TCPPing.create(SeedData.tcpping.subscriptionLine) shouldBe Some(SeedData.tcpping.expected)
    }

    "convert an entry from a subscription into an HTTP object" in {
      HTTP.create(SeedData.http.subscriptionLine) shouldBe Some(SeedData.http.expected)
    }

    "convert many entries from a subscription into a Flow object" in {
      SeedData.bigdata.flowsAsLineProtocol.map(l => Flow.create(l).get) shouldBe SeedData.bigdata.flowsExpected
    }
  }

  "InfluxMeasurementFactory.createMeasurement" should {
    "convert several entries to their respective Measurement subclasses" in {
      Seq(
        InfluxMeasurementFactory.createMeasurement(SeedData.icmp.subscriptionLine),
        InfluxMeasurementFactory.createMeasurement(SeedData.dns.subscriptionLine),
        InfluxMeasurementFactory.createMeasurement(SeedData.traceroutePathlen.subscriptionLine),
        InfluxMeasurementFactory.createMeasurement(SeedData.tcpping.subscriptionLine),
        InfluxMeasurementFactory.createMeasurement(SeedData.http.subscriptionLine)
      ).foreach {
        case Some(x) =>
          x match {
            case _: ICMP => x shouldBe SeedData.icmp.expected
            case _: DNS => x shouldBe SeedData.dns.expected
            case _: TraceroutePathlen => x shouldBe SeedData.traceroutePathlen.expected
            case _: TCPPing => x shouldBe SeedData.tcpping.expected
            case _: HTTP => x shouldBe SeedData.http.expected
            case _             => fail("Created a type we didn't recognise")
          }
        case None => fail("Failed to create an object")
      }
    }
  }
}
