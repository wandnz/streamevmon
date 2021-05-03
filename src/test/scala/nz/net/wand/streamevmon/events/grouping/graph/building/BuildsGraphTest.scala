/* This file is part of streamevmon.
 *
 * Copyright (C) 2021  The University of Waikato, Hamilton, New Zealand
 *
 * Author: Daniel Oosterwijk
 *
 * All rights reserved.
 *
 * This code has been developed by the University of Waikato WAND
 * research group. For further information please see https://wand.nz,
 * or our Github organisation at https://github.com/wanduow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.net.wand.streamevmon.events.grouping.graph.building

import nz.net.wand.streamevmon.events.grouping.graph.building.GraphChangeEvent._
import nz.net.wand.streamevmon.events.grouping.graph.impl.GraphType._
import nz.net.wand.streamevmon.test.TestBase
import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.events.grouping.graph.GraphDotExporter

import java.time.Instant

import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.operators.ProcessOperator
import org.apache.flink.streaming.util.OneInputStreamOperatorTestHarness
import org.apache.flink.util.Collector

import scala.collection.JavaConverters._

class BuildsGraphTest extends TestBase {
  def buildsGraphBehaviours(implementationBuilder: Unit => BuildsGraph with ProcessFunction[GraphChangeEvent, GraphChangeEvent]): Unit = {
    val v1 =
      new VertexT(
        Set("abc.example.org"),
        Set(),
        Set(),
        None
      )

    val v2 = new VertexT(
      Set("xyz.example.org"),
      Set(),
      Set(),
      None
    )

    val v3 = new VertexT(
      Set("qrs.example.org"),
      Set(),
      Set(),
      None
    )

    // We apply a bunch of events, which uses most of the basic options.
    // We don't need to try all of them here, since they're tested over
    // in GraphChangeEventTest.
    // This progressively builds and tears down parts of the graph,
    // resulting in a single-directional loop between v1 -> v2 -> v3 -> v1.
    val cachedEdge = new EdgeT(Instant.ofEpochMilli(1000), "1000")
    val buildSteps = Seq(
      AddVertex(v1),
      AddVertex(v2),
      DoNothing(),
      AddOrUpdateEdge(v1, v2, new EdgeT(Instant.EPOCH, "1")),
      RemoveVertex(v2), // edge goes away as well
      AddVertex(v3),
      DoNothing(),
      AddOrUpdateEdge(v3, v1, cachedEdge),
      UpdateVertex.create(v3, v2), // edge should not go away yet
      DoNothing(),
      RemoveEdge(cachedEdge),
      AddVertex(v3),
      AddOrUpdateEdge(v1, v3, new EdgeT(Instant.EPOCH, "2")),
      RemoveEdgeByVertices(v1, v3),
      AddOrUpdateEdge(v1, v2, new EdgeT(Instant.EPOCH, "3")),
      AddOrUpdateEdge(v2, v3, new EdgeT(Instant.EPOCH, "4")),
      AddOrUpdateEdge(v3, v1, new EdgeT(Instant.EPOCH, "5")),
      DoNothing()
    )

    def ensureLayout(graph: GraphT): Unit = {
      // Just make sure it ended up with the right topology. All the
      // intermediary steps can be assumed to have applied.
      graph.vertexSet.asScala shouldBe Set(v1, v2, v3)
      graph.outgoingEdgesOf(v1).forEach { e =>
        graph.getEdgeTarget(e) shouldBe v2
      }
      graph.outgoingEdgesOf(v2).forEach { e =>
        graph.getEdgeTarget(e) shouldBe v3
      }
      graph.outgoingEdgesOf(v3).forEach { e =>
        graph.getEdgeTarget(e) shouldBe v1
      }
    }

    "build graphs from GraphChangeEvents" in {
      val impl = implementationBuilder(Unit)

      buildSteps.foreach(impl.receiveGraphChangeEvent)

      ensureLayout(impl.graph)
    }

    "restore from checkpoints" in {
      val impl = implementationBuilder(Unit)
      val harness = new OneInputStreamOperatorTestHarness(new ProcessOperator(impl))
      harness.getExecutionConfig.setGlobalJobParameters(Configuration.get())
      harness.open()
      var currentTime = 1000

      buildSteps.foreach { event =>
        harness.processElement(event, currentTime)
        currentTime += 1
      }
      ensureLayout(impl.graph)

      val snapshot = harness.snapshot(1, currentTime)
      harness.close()

      val newImpl = implementationBuilder(Unit)
      val newHarness = new OneInputStreamOperatorTestHarness(new ProcessOperator(newImpl))
      newHarness.getExecutionConfig.setGlobalJobParameters(Configuration.get())
      newHarness.setup()
      newHarness.initializeState(snapshot)
      newHarness.open()

      ensureLayout(newImpl.graph)
    }
  }

  "Basic implementation of BuildsGraph" should {
    class BasicImpl
      extends ProcessFunction[GraphChangeEvent, GraphChangeEvent]
              with BuildsGraph
              with CheckpointedFunction {

      override def processElement(
        value: GraphChangeEvent,
        ctx  : ProcessFunction[GraphChangeEvent, GraphChangeEvent]#Context,
        out  : Collector[GraphChangeEvent]
      ): Unit = {
        receiveGraphChangeEvent(value)
      }

      override def snapshotState(context: FunctionSnapshotContext): Unit = {
        snapshotGraphState(context)
      }

      override def initializeState(context: FunctionInitializationContext): Unit = {
        initializeGraphState(context)
      }
    }

    behave like buildsGraphBehaviours(_ => new BasicImpl())
  }

  "GraphPruneParallelAnonymousHostEventGenerator" should {
    behave like buildsGraphBehaviours(_ => new GraphPruneParallelAnonymousHostEventGenerator())
  }

  "GraphDotExporter" should {
    behave like buildsGraphBehaviours(_ => new GraphDotExporter())
  }
}
