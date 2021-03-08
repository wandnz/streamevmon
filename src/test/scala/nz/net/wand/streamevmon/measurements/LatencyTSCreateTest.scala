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

import nz.net.wand.streamevmon.measurements.latencyts._
import nz.net.wand.streamevmon.test.{SeedData, TestBase}

class LatencyTSCreateTest extends TestBase {
  "AMP ICMP result" should {
    "convert into a LatencyTSAmpICMP object" in {
      LatencyTSAmpICMP.create(SeedData.latencyTs.ampLine, SeedData.latencyTs.amp.stream) shouldBe SeedData.latencyTs.amp
    }
  }

  "Smokeping result" should {
    "convert into a LatencyTSSmokeping object" when {
      "no loss detected" in {
        LatencyTSSmokeping.create(
          SeedData.latencyTs.smokepingLineNoLoss,
          SeedData.latencyTs.smokepingNoLoss.stream
        ) shouldBe SeedData.latencyTs.smokepingNoLoss
      }

      "some loss detected" in {
        LatencyTSSmokeping.create(
          SeedData.latencyTs.smokepingLineSomeLoss,
          SeedData.latencyTs.smokepingSomeLoss.stream
        ) shouldBe SeedData.latencyTs.smokepingSomeLoss
      }

      "total loss detected" in {
        LatencyTSSmokeping.create(
          SeedData.latencyTs.smokepingLineAllLoss,
          SeedData.latencyTs.smokepingAllLoss.stream
        ) shouldBe SeedData.latencyTs.smokepingAllLoss
      }

      "no data is present" in {
        LatencyTSSmokeping.create(
          SeedData.latencyTs.smokepingLineNoEntry,
          SeedData.latencyTs.smokepingNoEntry.stream
        ) shouldBe SeedData.latencyTs.smokepingNoEntry
      }

      "loss value does not match number of results" in {
        LatencyTSSmokeping.create(
          SeedData.latencyTs.smokepingLineMismatchedLoss,
          SeedData.latencyTs.smokepingMismatchedLoss.stream
        ) shouldBe SeedData.latencyTs.smokepingMismatchedLoss
      }
    }
  }
}
