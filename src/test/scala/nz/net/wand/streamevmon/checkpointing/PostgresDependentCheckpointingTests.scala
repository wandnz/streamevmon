package nz.net.wand.streamevmon.checkpointing

import nz.net.wand.streamevmon.{HarnessingTest, PostgresContainerSpec, SeedData}
import nz.net.wand.streamevmon.events.grouping.graph.TracerouteAsInetPathExtractor
import nz.net.wand.streamevmon.measurements.MeasurementMetaExtractor
import nz.net.wand.streamevmon.measurements.amp.{Traceroute, TracerouteMeta}

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord

class PostgresDependentCheckpointingTests extends HarnessingTest with PostgresContainerSpec {
  "ProcessFunctions that require PostgreSQL" should {
    "restore from checkpoints correctly" when {
      "type is TracerouteAsInetPathExtractor" in {
        val extractor: TracerouteAsInetPathExtractor = new TracerouteAsInetPathExtractor
        extractor.pgCon = getPostgres

        var harness = newHarness(extractor)
        harness.open()

        currentTime += 1
        harness.processElement1(SeedData.traceroute.expected, currentTime)
        currentTime += 1
        harness.processElement1(SeedData.traceroute.expected, currentTime)
        currentTime += 1
        harness.processElement1(SeedData.traceroute.expected, currentTime)

        // no output with an unknown meta
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness, extractor)

        currentTime += 1
        harness.processElement2(SeedData.traceroute.expectedMeta, currentTime)

        // adding a matching meta should produce outputs for all previous inputs
        harness.getOutput should have size 3

        harness = snapshotAndRestart(harness, extractor)

        currentTime += 1
        harness.processElement1(SeedData.traceroute.expected, currentTime)

        // output gets cleared when we snapshot and restart the second time,
        // but the meta should be retained
        harness.getOutput should have size 1
      }

      "type is MeasurementMetaExtractor" in {
        val extractor = new MeasurementMetaExtractor[Traceroute, TracerouteMeta]()
        var harness = newHarness(extractor)
        harness.open()
        extractor.pgCon = getPostgres

        currentTime += 1
        harness.processElement(SeedData.traceroute.expected, currentTime)

        val output = harness.getSideOutput(extractor.outputTag).toArray
        output should have size 1
        output.head.asInstanceOf[StreamRecord[TracerouteMeta]].getValue shouldBe SeedData.traceroute.expectedMeta

        harness.processElement(SeedData.traceroute.expected, currentTime)

        output should have size 1
        output.head.asInstanceOf[StreamRecord[TracerouteMeta]].getValue shouldBe SeedData.traceroute.expectedMeta

        harness = snapshotAndRestart(harness, extractor)

        harness.processElement(SeedData.traceroute.expected, currentTime)

        // If there hasn't been a new entry in the side output since the harness
        // was started, the output is never registered, and comes out as null.
        val newSideOutput = harness.getSideOutput(extractor.outputTag)
        if (newSideOutput != null) {
          newSideOutput should have size 0
        }
      }
    }
  }
}
