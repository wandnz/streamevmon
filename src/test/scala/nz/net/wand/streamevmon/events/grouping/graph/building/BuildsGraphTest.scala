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
import nz.net.wand.streamevmon.events.grouping.graph.GraphType._
import nz.net.wand.streamevmon.test.TestBase

import java.time.Instant

import scala.collection.JavaConverters._

class BuildsGraphTest extends TestBase {
  class Harness extends BuildsGraph

  "BuildsGraph" should {
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

    "build graphs from GraphChangeEvents" in {
      val harness = new Harness()

      val cachedEdge = new EdgeT(Instant.ofEpochMilli(1000), "1000")

      // We apply a bunch of events, which should use all the options.
      // This progressively builds and tears down parts of the graph,
      // resulting in a single-directional loop between v1 -> v2 -> v3 -> v1.
      Seq(
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
        .foreach(harness.receiveGraphChangeEvent)

      // Just make sure it ended up with the right topology. All the
      // intermediary steps can be assumed to have applied.
      harness.graph.vertexSet.asScala shouldBe Set(v1, v2, v3)
      harness.graph.outgoingEdgesOf(v1).forEach { e =>
        harness.graph.getEdgeTarget(e) shouldBe v2
      }
      harness.graph.outgoingEdgesOf(v2).forEach { e =>
        harness.graph.getEdgeTarget(e) shouldBe v3
      }
      harness.graph.outgoingEdgesOf(v3).forEach { e =>
        harness.graph.getEdgeTarget(e) shouldBe v1
      }
    }
  }
}
