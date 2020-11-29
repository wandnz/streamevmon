package nz.net.wand.streamevmon.checkpointing

import nz.net.wand.streamevmon.{PostgresContainerSpec, SeedData}
import nz.net.wand.streamevmon.connectors.postgres.schema.AsInetPath
import nz.net.wand.streamevmon.events.grouping.graph.{TracerouteAsInetPathExtractor, TraceroutePathGraph}
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.MeasurementMetaExtractor
import nz.net.wand.streamevmon.measurements.amp.{Traceroute, TracerouteMeta}

import org.apache.flink.streaming.api.scala._

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

      "type is MeasurementMetaExtractor" in {
        val extractor = new MeasurementMetaExtractor[Traceroute, TracerouteMeta]()
        extractor.pgCon = getPostgres

        var harness = newHarness(extractor)
        harness.open()

        lastGeneratedTime += 1
        harness.processElement(SeedData.traceroute.expected, lastGeneratedTime)

        var output = harness.getSideOutput(extractor.outputTag).toArray
        output should have size 1
        output.head shouldBe SeedData.traceroute.expectedMeta

        harness.processElement(SeedData.traceroute.expected, lastGeneratedTime)

        output = harness.getSideOutput(extractor.outputTag).toArray
        output should have size 1
        output.head shouldBe SeedData.traceroute.expectedMeta

        harness = snapshotAndRestart(harness, extractor)

        harness.processElement(SeedData.traceroute.expected, lastGeneratedTime)

        output = harness.getSideOutput(extractor.outputTag).toArray
        output should have size 0
      }

      "type is TraceroutePathGraph" in {
        // We need to get an AsInetPath to pass to the TraceroutePathGraph.
        val asInetPathExtractor = new TracerouteAsInetPathExtractor()
        asInetPathExtractor.pgCon = getPostgres

        val asInetHarness = newHarness(asInetPathExtractor)
        asInetHarness.open()

        lastGeneratedTime += 1
        asInetHarness.processElement2(SeedData.traceroute.expectedMeta, lastGeneratedTime)
        lastGeneratedTime += 1
        asInetHarness.processElement1(SeedData.traceroute.expected, lastGeneratedTime)

        asInetHarness.extractOutputValues should have size 1
        val path: AsInetPath = asInetHarness.extractOutputValues.get(0)

        // Now we can use the graph.
        val graph = new TraceroutePathGraph[Event]()

        var harness = newHarness(graph)
        harness.open()

        lastGeneratedTime += 1
        harness.processElement2(path, lastGeneratedTime)

        // We can do this since there are no entries that need merging in the
        // example path.
        graph.mergedHosts should have size path.size
        graph.graph.vertexSet should have size path.size

        harness = snapshotAndRestart(harness, graph)

        graph.mergedHosts should have size path.size
        graph.graph.vertexSet should have size path.size
      }
    }
  }
}
