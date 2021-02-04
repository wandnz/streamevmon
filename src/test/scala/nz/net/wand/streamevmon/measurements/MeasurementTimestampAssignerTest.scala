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

import nz.net.wand.streamevmon.{SeedData, TestBase}

class MeasurementTimestampAssignerTest extends TestBase {
  "Correct timestamp should be assigned" in {
    val ts = new MeasurementTimestampAssigner

    val flattenedItems = Seq(
      SeedData.bigdata.flowsExpected,
      SeedData.esmond.expectedObjects.flatMap { objs =>
        Seq(objs.baseMeasurement, objs.baseRichMeasurement, objs.summaryRichMeasurement)
      }
    ).flatten

    (Seq(
      SeedData.icmp.expected,
      SeedData.icmp.lossyExpected,
      SeedData.icmp.expectedRich,
      SeedData.dns.expected,
      SeedData.dns.lossyExpected,
      SeedData.dns.expectedRich,
      SeedData.traceroute.expected,
      SeedData.traceroute.expectedRich,
      SeedData.traceroutePathlen.expected,
      SeedData.traceroutePathlen.expectedRich,
      SeedData.tcpping.expected,
      SeedData.tcpping.lossyExpected,
      SeedData.tcpping.expectedRich,
      SeedData.http.expected,
      SeedData.http.lossyExpected,
      SeedData.http.expectedRich,
      SeedData.latencyTs.amp,
      SeedData.latencyTs.smokepingNoLoss,
      SeedData.latencyTs.smokepingSomeLoss,
      SeedData.latencyTs.smokepingAllLoss,
      SeedData.latencyTs.smokepingNoEntry,
      SeedData.latencyTs.smokepingMismatchedLoss,
      SeedData.nab.expected
    ) ++ flattenedItems).foreach { meas =>
      ts.extractTimestamp(meas, 0L) shouldBe meas.time.toEpochMilli
    }
  }
}
