package nz.net.wand.streamevmon.checkpointing

import nz.net.wand.streamevmon.{PostgresContainerSpec, SeedData}
import nz.net.wand.streamevmon.events.grouping.graph.TracerouteAsInetPathExtractor

class PostgresProcessFunctionCheckpointingTests extends CheckpointingTestBase with PostgresContainerSpec {
  "ProcessFunctions that require PostgreSQL" should {
    "restore from checkpoints correctly" when {
      "type is TracerouteAsInetPathExtractor" in {
        val extractor: TracerouteAsInetPathExtractor = new TracerouteAsInetPathExtractor
        extractor.pgCon = getPostgres

        var harness = newHarness(extractor)
        harness.open()

        lastGeneratedTime += 1
        harness.processElement1(SeedData.traceroute.expected, lastGeneratedTime)
        lastGeneratedTime += 1
        harness.processElement1(SeedData.traceroute.expected, lastGeneratedTime)
        lastGeneratedTime += 1
        harness.processElement1(SeedData.traceroute.expected, lastGeneratedTime)

        // no output with an unknown meta
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness, extractor)

        lastGeneratedTime += 1
        harness.processElement2(SeedData.traceroute.expectedMeta, lastGeneratedTime)

        // adding a matching meta should produce outputs for all previous inputs
        harness.getOutput should have size 3

        harness = snapshotAndRestart(harness, extractor)

        lastGeneratedTime += 1
        harness.processElement1(SeedData.traceroute.expected, lastGeneratedTime)

        // output gets cleared when we snapshot and restart the second time,
        // but the meta should be retained
        harness.getOutput should have size 1
      }

      "type is PostgresTracerouteSourceFunction" in {

      }
    }
  }
}
