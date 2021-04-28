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

package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.connectors.postgres.schema.AsInetPath
import nz.net.wand.streamevmon.events.grouping.graph.building.TracerouteAsInetPathExtractor
import nz.net.wand.streamevmon.test.{HarnessingTest, PostgresContainerSpec, SeedData}

import org.apache.flink.streaming.runtime.streamrecord.StreamRecord

import scala.collection.JavaConverters._

class TracerouteAsInetPathExtractorTest extends PostgresContainerSpec with HarnessingTest {
  "TracerouteAsInetPathExtractor" should {
    "extract correct AsInetPaths" in {
      val extractor = new TracerouteAsInetPathExtractor()
      val harness = newHarness(extractor)
      harness.open()
      extractor.pgCon = getPostgres

      currentTime += 1
      harness.processElement1(SeedData.traceroute.expected, currentTime)

      // no output with an unknown meta
      harness.getOutput should have size 0

      currentTime += 1
      harness.processElement2(SeedData.traceroute.expectedMeta, currentTime)

      harness.getOutput should have size 1
      harness.getOutput.asScala.head
        .asInstanceOf[StreamRecord[AsInetPath]].getValue shouldBe
        SeedData.traceroute.expectedAsInetPath
    }
  }
}
