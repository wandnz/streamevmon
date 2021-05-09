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

package nz.net.wand.streamevmon.checkpointing

import nz.net.wand.streamevmon.events.grouping.graph.building.TracerouteAsInetPathExtractor
import nz.net.wand.streamevmon.measurements.{MeasurementMetaExtractor, MeasurementMetaTogetherExtractor}
import nz.net.wand.streamevmon.measurements.amp.{Traceroute, TracerouteMeta}
import nz.net.wand.streamevmon.test.{HarnessingTest, PostgresContainerSpec, SeedData}

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord

import scala.collection.JavaConverters._

class PostgresDependentCheckpointingTests extends PostgresContainerSpec with HarnessingTest {
  "ProcessFunctions that require PostgreSQL" should {
    "restore from checkpoints correctly" when {
      "type is TracerouteAsInetPathExtractor" in {
        def extractor: TracerouteAsInetPathExtractor = {
          val e = new TracerouteAsInetPathExtractor
          e.pgCon = getPostgres
          e
        }

        var harness = newHarness(extractor)
        harness.open()

        currentTime += 1
        harness.processElement1(SeedData.traceroute.expected, currentTime)
        currentTime += 1
        harness.processElement1(SeedData.traceroute.expected, currentTime)
        currentTime += 1
        harness.processElement1(SeedData.traceroute.expected, currentTime)

        // no output with an unknown meta
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness, extractor)

        currentTime += 1
        harness.processElement2(SeedData.traceroute.expectedMeta, currentTime)

        // adding a matching meta should produce outputs for all previous inputs
        harness.getOutput should have size 3

        harness = snapshotAndRestart(harness, extractor)

        currentTime += 1
        harness.processElement1(SeedData.traceroute.expected, currentTime)

        // output gets cleared when we snapshot and restart the second time,
        // but the meta should be retained
        harness.getOutput should have size 1
      }

      "type is MeasurementMetaExtractor" in {
        val extractor = new MeasurementMetaExtractor[Traceroute, TracerouteMeta]()
        var harness = newHarness(extractor)
        harness.open()
        extractor.pgCon = getPostgres

        currentTime += 1
        harness.processElement(SeedData.traceroute.expected, currentTime)

        val output = harness.getSideOutput(extractor.outputTag).toArray
        output should have size 1
        output.head.asInstanceOf[StreamRecord[TracerouteMeta]].getValue shouldBe SeedData.traceroute.expectedMeta

        harness.processElement(SeedData.traceroute.expected, currentTime)

        val output2 = harness.getSideOutput(extractor.outputTag).toArray
        output2 should have size 1
        output2.head.asInstanceOf[StreamRecord[TracerouteMeta]].getValue shouldBe SeedData.traceroute.expectedMeta

        val extractor2 = new MeasurementMetaExtractor[Traceroute, TracerouteMeta]()
        harness = snapshotAndRestart(harness, extractor2)

        harness.processElement(SeedData.traceroute.expected, currentTime)

        // If there hasn't been a new entry in the side output since the harness
        // was started, the output is never registered, and comes out as null.
        val newSideOutput = harness.getSideOutput(extractor2.outputTag)
        if (newSideOutput != null) {
          newSideOutput should have size 0
        }
      }

      "type is MeasurementMetaTogetherExtractor" in {
        val extractor = new MeasurementMetaTogetherExtractor[Traceroute, TracerouteMeta]()
        var harness = newHarness(extractor)
        harness.open()
        extractor.pgCon = getPostgres

        currentTime += 1
        harness.processElement(SeedData.traceroute.expected, currentTime)
        harness.extractOutputValues should have size 1
        harness.extractOutputValues.asScala.head shouldBe(SeedData.traceroute.expected, SeedData.traceroute.expectedMeta)

        currentTime += 1
        harness.processElement(SeedData.traceroute.expected, currentTime)
        harness.extractOutputValues should have size 2
        harness.extractOutputValues.get(0) shouldBe harness.extractOutputValues.get(1)

        val extractor2 = new MeasurementMetaTogetherExtractor[Traceroute, TracerouteMeta]()
        harness = snapshotAndRestart(harness, extractor2)

        currentTime += 1
        harness.processElement(SeedData.traceroute.expected, currentTime)
        harness.extractOutputValues should have size 1
        harness.extractOutputValues.asScala.head shouldBe(SeedData.traceroute.expected, SeedData.traceroute.expectedMeta)
      }
    }
  }
}
