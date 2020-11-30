package nz.net.wand.streamevmon.measurements

import nz.net.wand.streamevmon.{HarnessingTest, PostgresContainerSpec, SeedData}
import nz.net.wand.streamevmon.measurements.traits.{Measurement, PostgresMeasurementMeta}

import org.apache.flink.streaming.api.scala._

class MeasurementMetaExtractorTest extends PostgresContainerSpec with HarnessingTest {
  "MeasurementMetaExtractor" should {
    "extract correct Metas" in {
      val extractor = new MeasurementMetaExtractor[Measurement, PostgresMeasurementMeta]()

      val harness = newHarness(extractor)
      harness.open()
      extractor.pgCon = getPostgres

      Seq(
        (SeedData.icmp.expected, SeedData.icmp.expectedMeta),
        (SeedData.dns.expected, SeedData.dns.expectedMeta),
        (SeedData.http.expected, SeedData.http.expectedMeta),
        (SeedData.traceroute.expected, SeedData.traceroute.expectedMeta),
        (SeedData.tcpping.expected, SeedData.tcpping.expectedMeta),
      ).foreach { case (meas, meta) =>
        currentTime += 1
        harness.processElement(meas, currentTime)
        harness.getSideOutput(extractor.outputTag).remove().getValue shouldBe meta
      }
    }
  }
}
