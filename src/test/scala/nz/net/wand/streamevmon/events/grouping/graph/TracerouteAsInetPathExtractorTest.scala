package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.{HarnessingTest, PostgresContainerSpec, SeedData}
import nz.net.wand.streamevmon.connectors.postgres.schema.AsInetPath

import org.apache.flink.streaming.runtime.streamrecord.StreamRecord

import scala.collection.JavaConverters._

class TracerouteAsInetPathExtractorTest extends PostgresContainerSpec with HarnessingTest {
  "TracerouteAsInetPathExtractor" should {
    "extract correct AsInetPaths" in {
      val extractor = new TracerouteAsInetPathExtractor()
      val harness = newHarness(extractor)
      harness.open()
      extractor.pgCon = getPostgres

      currentTime += 1
      harness.processElement1(SeedData.traceroute.expected, currentTime)

      // no output with an unknown meta
      harness.getOutput should have size 0

      currentTime += 1
      harness.processElement2(SeedData.traceroute.expectedMeta, currentTime)

      harness.getOutput should have size 1
      harness.getOutput.asScala.head
        .asInstanceOf[StreamRecord[AsInetPath]].getValue shouldBe
        SeedData.traceroute.expectedAsInetPath
    }
  }
}
