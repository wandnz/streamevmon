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

import nz.net.wand.streamevmon.connectors.postgres.schema.AsNumber
import nz.net.wand.streamevmon.events.grouping.graph.building.GraphChangeEvent._
import nz.net.wand.streamevmon.events.grouping.graph.impl.GraphType.{EdgeT, VertexT}
import nz.net.wand.streamevmon.test.HarnessingTest

import java.net.InetAddress
import java.time.Instant

import org.apache.flink.streaming.runtime.streamrecord.StreamRecord

import scala.collection.JavaConverters._

class GraphChangeAliasResolutionTest extends HarnessingTest {
  "GraphChangeAliasResolution" should {
    "pass events through" when {
      val v1 = new VertexT(
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

      "no vertices can be merged" in {
        val func = new GraphChangeAliasResolution
        val harness = newHarness(func)

        val cachedEdge = new EdgeT(Instant.ofEpochMilli(1000), "1000")
        val events = Seq(
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

        harness.open()
        var timestamp = 1000
        events.foreach { e =>
          harness.processElement(e, timestamp)
          timestamp += 1
        }

        harness
          .getOutput
          .asScala
          .asInstanceOf[Iterable[StreamRecord[GraphChangeEvent]]]
          .map(_.getValue) shouldBe events
      }
    }

    val v1 = new VertexT(
      Set("abc.example.org"),
      Set((InetAddress.getByName("123.123.123.123"), AsNumber.Unknown)),
      Set(),
      None
    )

    val v2 = new VertexT(
      Set("xyz.example.org"),
      Set((InetAddress.getByName("212.212.212.212"), AsNumber.Unknown)),
      Set(),
      None
    )

    val v3 = new VertexT(
      Set("abc.example.org"),
      Set(
        (InetAddress.getByName("100.100.100.100"), AsNumber.Unknown)
      ),
      Set(),
      None
    )

    val v4 = new VertexT(
      Set("abc.example.org"),
      Set(
        (InetAddress.getByName("123.123.123.123"), AsNumber.Unknown),
        (InetAddress.getByName("100.100.100.100"), AsNumber.Unknown)
      ),
      Set(),
      None
    )

    "alter events after an alias is spotted" in {
      val buildFunctions = Seq(
        v => AddVertex(v),
        v => RemoveVertex(v),
        v => UpdateVertex.create(v, v.copy(addresses = Set())),
        v => AddOrUpdateEdge(v, v, null),
        v => RemoveEdgeByVertices(v, v)
      )
      val vertexSendOrder = Seq(v1, v2, v3, v1, v2, v3)
      val expectedVertices = Seq(v1, v2, v4, v4, v2, v4)

      buildFunctions.foreach { func =>
        val processFunc = new GraphChangeAliasResolution()
        val harness = newHarness(processFunc)
        harness.open()
        var currentTime = 1000
        vertexSendOrder.foreach { v =>
          harness.processElement(func(v), currentTime)
          currentTime += 1
        }

        harness
          .getOutput
          .asScala
          .asInstanceOf[Iterable[StreamRecord[GraphChangeEvent]]]
          .zip(expectedVertices)
          .foreach { case (record, expected) =>
            record.getValue match {
              case AddVertex(vertex) => vertex shouldBe expected
              case RemoveVertex(vertex) => vertex shouldBe expected
              case UpdateVertex(vertex1, _) =>
                vertex1 shouldBe expected
              // yes, vertex2 here is untested. can't figure a nice way to do it
              case AddOrUpdateEdge(start, end, _) =>
                start shouldBe expected
                end shouldBe expected
              case RemoveEdgeByVertices(start, end) =>
                start shouldBe expected
                end shouldBe expected
              case RemoveEdge(_) =>
              case MeasurementEndMarker(_) =>
              case RemoveOldEdges(_) =>
              case _: NoArgumentGraphChangeEvent =>
              case MergeVertices(_) =>
            }
          }
      }
    }

    "interpret known aliases when given a MergeVertices" in {
      val eventsToSend = Seq(
        AddVertex(v1),
        AddVertex(v2),
        MergeVertices(Set(v1, v2)),
        AddVertex(v1),
        AddVertex(v2),
      )

      val expectedResults = Seq(
        AddVertex(v1),
        AddVertex(v2),
        MergeVertices(Set(v1, v2)),
        AddVertex(v1.mergeWith(v2)),
        AddVertex(v1.mergeWith(v2))
      )

      val processFunc = new GraphChangeAliasResolution()
      val harness = newHarness(processFunc)
      harness.open()
      eventsToSend.foreach { v =>
        harness.processElement(v, currentTime)
        currentTime += 1
      }

      harness
        .extractOutputValues()
        .asScala shouldBe expectedResults
    }

    "not output events that aren't part of the graph after a few MergeVertices" in {
      def v(id: Int): VertexT = new VertexT(
        Set(),
        Set(),
        Set((id, id, id)),
        None
      )

      val events = Seq(
        AddVertex(v(1)),
        AddVertex(v(2)),
        AddVertex(v(3)),
        AddVertex(v(4)),
        MergeVertices(Set(v(1), v(2))),
        MergeVertices(Set(v(3), v(4))),
        MergeVertices(Set(v(1), v(4))),
        MergeVertices(Set(v(2), v(3))),
        AddVertex(v(1)),
        MergeVertices(Set(v(1), v(2)))
      )

      val func = new GraphChangeAliasResolution()
      val harness = newHarness(func)
      harness.open()
      events.foreach { v =>
        harness.processElement(v, currentTime)
        currentTime += 1
      }

      val grapher = new BasicBuildsGraphImpl()
      val grapherHarness = newHarness(grapher)
      grapherHarness.open()

      noException shouldBe thrownBy {
        harness.extractOutputValues.forEach { v =>
          grapherHarness.processElement(v, currentTime)
          currentTime += 1
        }
      }
    }
  }
}
