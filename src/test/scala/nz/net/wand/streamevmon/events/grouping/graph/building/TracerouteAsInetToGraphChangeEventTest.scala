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

import nz.net.wand.streamevmon.connectors.postgres.schema.AsInetPathEntry
import nz.net.wand.streamevmon.events.grouping.graph.building.GraphChangeEvent.{AddOrUpdateEdge, AddVertex, MeasurementEndMarker}
import nz.net.wand.streamevmon.events.grouping.graph.GraphType.VertexT
import nz.net.wand.streamevmon.test.{HarnessingTest, SeedData}

import org.apache.flink.streaming.runtime.streamrecord.StreamRecord

import scala.collection.JavaConverters._

class TracerouteAsInetToGraphChangeEventTest extends HarnessingTest {
  "TracerouteAsInetToGraphChangeEvent" should {
    "convert AsInetPaths to GraphChangeEvents" in {
      val func = new TracerouteAsInetToGraphChangeEvent()
      val harness = newHarness(func)
      harness.open()

      harness.processElement(SeedData.traceroute.expectedAsInetPath, currentTime)
      currentTime += 1
      harness.endInput()

      val output = harness.getOutput.asScala.asInstanceOf[Iterable[StreamRecord[GraphChangeEvent]]].map(_.getValue)

      def vertexRepresentsPathEntry(v: VertexT, e: AsInetPathEntry): Boolean = {
        if (e.address.isDefined) {
          v.addresses.contains((e.address.get, e.as))
        }
        else {
          v.ampTracerouteUids.nonEmpty
        }
      }

      // First, test that all the AddVertex entries are valid
      SeedData.traceroute.expectedAsInetPath.foreach { entry =>
        output
          .find {
            case AddVertex(vertex) => vertexRepresentsPathEntry(vertex, entry)
            case _ => false
          } shouldBe defined
      }

      // Next, make sure the AddOrUpdateEdge entries are valid
      SeedData.traceroute.expectedAsInetPath.sliding(2).foreach { elems =>
        output
          .find {
            case AddOrUpdateEdge(start, end, _) =>
              vertexRepresentsPathEntry(start, elems.head) && vertexRepresentsPathEntry(end, elems.drop(1).head)
            case _ => false
          } shouldBe defined
      }

      // Finally, make sure it ends with a MeasurementEndMarker.
      output.last shouldBe a[MeasurementEndMarker]
      output.last.asInstanceOf[MeasurementEndMarker].timeOfMeasurement shouldBe
        SeedData.traceroute.expectedAsInetPath.measurement.time
    }
  }
}
