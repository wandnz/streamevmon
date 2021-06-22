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

package nz.net.wand.streamevmon.measurements

import nz.net.wand.streamevmon.measurements.traits.{Measurement, PostgresMeasurementMeta}
import nz.net.wand.streamevmon.test.{HarnessingTest, PostgresContainerSpec, SeedData}

import org.apache.flink.streaming.api.scala._

import scala.collection.JavaConverters._

class MeasurementMetaTogetherExtractorTest extends PostgresContainerSpec with HarnessingTest {
  "MeasurementMetaExtractor" should {
    "extract correct Metas" in {
      val extractor = new MeasurementMetaTogetherExtractor[Measurement, PostgresMeasurementMeta]()

      val harness = newHarness(extractor)
      harness.open()
      extractor.pgCon = getPostgres

      Seq(
        (SeedData.icmp.expected, SeedData.icmp.expectedMeta),
        (SeedData.dns.expected, SeedData.dns.expectedMeta),
        (SeedData.http.expected, SeedData.http.expectedMeta),
        (SeedData.traceroute.expected, SeedData.traceroute.expectedMeta),
        (SeedData.tcpping.expected, SeedData.tcpping.expectedMeta),
      ).foreach { case (meas, meta) =>
        currentTime += 1
        harness.processElement(meas, currentTime)
        harness.extractOutputValues.asScala.last shouldBe(meas, meta)
      }
    }
  }
}
