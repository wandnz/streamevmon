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
import nz.net.wand.streamevmon.events.grouping.graph.GraphType.{EdgeT, VertexT}
import nz.net.wand.streamevmon.test.HarnessingTest

import java.time.{Duration, Instant}

import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord
import org.apache.flink.util.Collector

import scala.collection.JavaConverters._

class GraphPruneEventGeneratorTest extends HarnessingTest {
  "GraphPruneEventGenerator" should {
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

    class Impl(onPrune: (Int, Instant) => Unit) extends GraphPruneEventGenerator {

      var eventsSinceLastPrune = 0
      var timeOfLastPrune = Instant.ofEpochMilli(1000)

      override def onProcessElement(value: GraphChangeEvent): Unit = {
        eventsSinceLastPrune += 1
      }

      override def doPrune(currentTime: Instant, ctx: ProcessFunction[GraphChangeEvent, GraphChangeEvent]#Context, out: Collector[GraphChangeEvent]): Unit = {
        onPrune(eventsSinceLastPrune, timeOfLastPrune)
        eventsSinceLastPrune = 0
        timeOfLastPrune = currentTime
      }
    }

    "pass all events through untouched" in {
      val func = new Impl((_: Int, _: Instant) => Unit)
      val harness = newHarness(func)

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

    "trigger a prune" when {
      "enough measurements have been sent" in {
        var timestamp = 1000

        def checkPrune(count: Int, time: Instant): Unit = {
          count shouldBe 500
          time shouldBe Instant.ofEpochMilli(timestamp - count)
        }

        val func = new Impl(checkPrune)
        val harness = newHarness(func)

        harness.open()
        Range(0, 1020).foreach { _ =>
          harness.processElement(events.head, timestamp)
          timestamp += 1
        }
      }

      "enough time has passed" in {
        var timestamp = 1000L

        def checkPrune(count: Int, time: Instant): Unit = {
          count shouldBe 12
          time shouldBe Instant.ofEpochMilli(timestamp).minus(Duration.ofHours(count))
        }

        val func = new Impl(checkPrune)
        val harness = newHarness(func)

        harness.open()
        Range(0, 30).foreach { _ =>
          harness.processElement(events.head, timestamp)
          timestamp += Duration.ofHours(1).toMillis
        }
      }
    }
  }
}
