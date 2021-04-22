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

package nz.net.wand.streamevmon.events.grouping.graph.pruning

import nz.net.wand.streamevmon.events.grouping.graph.{Host, NoReflectionUnusableEdgeSupplier}
import nz.net.wand.streamevmon.events.grouping.graph.GraphType._
import nz.net.wand.streamevmon.events.grouping.graph.building.GraphChangeEvent.{MeasurementEndMarker, RemoveOldEdges, RemoveUnconnectedVertices}
import nz.net.wand.streamevmon.events.grouping.graph.building.GraphPruneLastSeenTimeEventGenerator
import nz.net.wand.streamevmon.test.{HarnessingTest, TestBase}

import java.time.{Duration, Instant}

import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.AsUndirectedGraph

import scala.collection.JavaConverters._

class GraphPruneLastSeenTimeTest extends TestBase with HarnessingTest {

  val now = Instant.ofEpochMilli(1000000000000L)

  def getHost(id: Int): VertexT = {
    Host(
      Set(),
      Set(),
      Set((id, 0, 0)),
      None
    )
  }

  def getEdge(age: Duration): EdgeT = {
    new EdgeT(now.minus(age), now.toEpochMilli.toHexString)
  }

  def constructTestGraph: GraphT = {
    val graph = new GraphT(classOf[EdgeT])
    graph.setEdgeSupplier(new NoReflectionUnusableEdgeSupplier[EdgeT])

    Range(0, 25).foreach(id => graph.addVertex(getHost(id)))

    Range(0, 25).sliding(2).foreach { ids =>
      graph.addEdge(getHost(ids(0)), getHost(ids(1)), getEdge(Duration.ofHours(ids(1))))
    }

    graph
  }

  "GraphPruneLastSeenTime" should {
    "prune the correct edges" in {
      val pruner = new GraphPruneLastSeenTime[VertexT, EdgeT, GraphT](Duration.ofHours(12), now)

      val graph = constructTestGraph
      pruner.prune(graph)

      graph.edgeSet.asScala.foreach {
        _.lastSeen.compareTo(now.minus(Duration.ofHours(12))) should be >= 0
      }
    }

    "prune unconnected vertices" in {
      val pruner = new GraphPruneLastSeenTime[VertexT, EdgeT, GraphT](Duration.ofHours(12), now)

      val graph = constructTestGraph
      pruner.prune(graph)

      val allPaths = new DijkstraShortestPath(new AsUndirectedGraph(graph))
      graph.vertexSet.asScala.foreach { start =>
        graph.vertexSet.asScala.foreach { end =>
          allPaths.getPath(start, end) shouldNot be(null)
        }
      }
    }
  }

  "GraphPruneLastSeenTimeEventGenerator" should {
    "tell downstream actors to prune the correct items" in {
      val pruner = new GraphPruneLastSeenTimeEventGenerator()
      val harness = newHarness(pruner)
      harness.open()

      val currentTime = Instant.ofEpochSecond(100000L)
      Range(1, 505).foreach(_ => harness.processElement(MeasurementEndMarker(currentTime), currentTime.toEpochMilli))

      harness.extractOutputValues should contain(RemoveOldEdges(currentTime.minus(pruner.pruneAge)))
      harness.extractOutputValues should contain(RemoveUnconnectedVertices())
    }
  }
}
