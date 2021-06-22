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

import nz.net.wand.streamevmon.events.grouping.graph.building.GraphChangeEvent.{DoPruneParallelAnonymousHosts, MeasurementEndMarker}
import nz.net.wand.streamevmon.events.grouping.graph.building.GraphPruneParallelAnonymousHostEventGenerator
import nz.net.wand.streamevmon.events.grouping.graph.impl.{Host, NoReflectionUnusableEdgeSupplier}
import nz.net.wand.streamevmon.events.grouping.graph.impl.GraphType._
import nz.net.wand.streamevmon.test.{HarnessingTest, TestBase}

import java.time.Instant

import scala.collection.JavaConverters._

class GraphPruneParallelAnonymousHostTest extends TestBase with HarnessingTest {

  val topVertex: VertexT = Host(
    Set("Top"),
    Set(),
    Set(),
    None
  )

  val bottomVertex: VertexT = Host(
    Set("Bottom"),
    Set(),
    Set(),
    None
  )

  def getHost(id: Int): VertexT = {
    Host(
      Set(),
      Set(),
      Set((id, 0, 0)),
      None
    )
  }

  def getMergedHosts(ids: Int*): VertexT = {
    Host(
      Set(),
      Set(),
      ids.map(id => (id, 0, 0)).toSet,
      None
    )
  }

  var edgeCounter = 0

  def getEdge: EdgeT = {
    edgeCounter += 1
    new EdgeT(Instant.now(), edgeCounter.toString)
  }

  def constructTestGraph: GraphT = {
    val graph = new GraphT(classOf[EdgeT])
    graph.setEdgeSupplier(new NoReflectionUnusableEdgeSupplier[EdgeT])

    graph.addVertex(topVertex)
    Range(0, 20).foreach(id => graph.addVertex(getHost(id)))
    graph.addVertex(bottomVertex)

    Range(0, 20).foreach { id =>
      id % 4 match {
        case 0 => graph.addEdge(topVertex, getHost(id), getEdge)
        case 1 | 2 => graph.addEdge(getHost(id - 1), getHost(id), getEdge)
        case 3 =>
          graph.addEdge(getHost(id - 1), getHost(id), getEdge)
          graph.addEdge(getHost(id), bottomVertex, getEdge)
      }
    }

    graph
  }

  def constructExpectedGraph: GraphT = {
    val graph = new GraphT(classOf[EdgeT])
    graph.setEdgeSupplier(new NoReflectionUnusableEdgeSupplier[EdgeT])

    graph.addVertex(topVertex)
    val mergedHosts = Range(0, 4).map { id =>
      val host = getMergedHosts(id, id + 4, id + 8, id + 12, id + 16)
      graph.addVertex(host)
      host
    }
    graph.addVertex(bottomVertex)

    graph.addEdge(topVertex, mergedHosts.head, getEdge)
    mergedHosts.sliding(2).foreach { hosts =>
      graph.addEdge(hosts.head, hosts.drop(1).head, getEdge)
    }
    graph.addEdge(mergedHosts.last, bottomVertex, getEdge)

    graph
  }

  def comparableEdges(g: GraphT) = {
    g.edgeSet.asScala.map { e =>
      (g.getEdgeSource(e), g.getEdgeTarget(e))
    }
  }

  "GraphPruneParallelAnonymousHost" should {
    "merge hosts correctly" in {
      val graph = constructTestGraph
      val expected = constructExpectedGraph

      DoPruneParallelAnonymousHosts().apply(graph)

      graph.vertexSet shouldBe expected.vertexSet
      comparableEdges(graph) shouldBe comparableEdges(expected)
    }
  }

  "GraphPruneParallelAnonymousHostEventGenerator" should {
    "merge hosts correctly" in {
      val graph = constructTestGraph
      val expected = constructExpectedGraph
      val pruner = new GraphPruneParallelAnonymousHostEventGenerator()
      val harness = newHarness(pruner)
      harness.open()

      pruner.graph = graph

      Range(1, 505).foreach(_ => harness.processElement(MeasurementEndMarker(Instant.ofEpochMilli(100)), 100))

      pruner.graph.vertexSet shouldBe expected.vertexSet
      comparableEdges(pruner.graph) shouldBe comparableEdges(expected)
    }

    "send some events out of both pipes" in {
      val graph = constructTestGraph
      val pruner = new GraphPruneParallelAnonymousHostEventGenerator()
      val harness = newHarness(pruner)
      harness.open()

      pruner.graph = graph

      Range(1, 505).foreach(_ => harness.processElement(MeasurementEndMarker(Instant.ofEpochMilli(100)), 100))

      harness.extractOutputValues().asScala.find {
        case MeasurementEndMarker(_) => false
        case _ => true
      } shouldBe defined
      harness.getSideOutput(pruner.mergeVerticesEventOutputTag) should not have size(0)
    }
  }
}
