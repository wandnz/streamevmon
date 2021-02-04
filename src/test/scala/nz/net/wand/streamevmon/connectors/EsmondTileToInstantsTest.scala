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

package nz.net.wand.streamevmon.connectors

import nz.net.wand.streamevmon.TestBase
import nz.net.wand.streamevmon.connectors.esmond.EsmondAPI

import java.time._

class EsmondTileToInstantsTest extends TestBase {
  "EsmondAPI.tileToTimeRange" should {
    "convert tile IDs to Instant tuples" in {
      // Checks for a few reasonable values since the epoch
      // Netbeam only actually has data since around 2015, so most of these
      // will never happen.
      EsmondAPI.tileToTimeRange(10957) shouldEqual
        (
          LocalDate.ofYearDay(2000, 1).atStartOfDay(ZoneOffset.UTC).toInstant,
          LocalDate.ofYearDay(2000, 2).atStartOfDay(ZoneOffset.UTC).minus(Duration.ofNanos(1)).toInstant
        )

      EsmondAPI.tileToTimeRange(0) shouldEqual
        (
          LocalDate.ofYearDay(1970, 1).atStartOfDay(ZoneOffset.UTC).toInstant,
          LocalDate.ofYearDay(1970, 2).atStartOfDay(ZoneOffset.UTC).minus(Duration.ofNanos(1)).toInstant
        )

      EsmondAPI.tileToTimeRange(18443) shouldEqual
        (
          LocalDate.ofYearDay(2020, 182).atStartOfDay(ZoneOffset.UTC).toInstant,
          LocalDate.ofYearDay(2020, 183).atStartOfDay(ZoneOffset.UTC).minus(Duration.ofNanos(1)).toInstant
        )

      // These timestamps were grabbed from Netbeam (the source of tiles). The start of the range is inclusive...
      EsmondAPI.tileToTimeRange(18443)._1 shouldEqual Instant.ofEpochMilli(1593475200000L)
      // and the end of the range returned by our function is just before the start of the next range.
      // The last value that you'll actually get from Netbeam will be before the end of the range.
      Duration.between(Instant.ofEpochMilli(1593561570000L), EsmondAPI.tileToTimeRange(18443)._2) should be < Duration.ofSeconds(31)
    }

    "parse strings correctly" in {
      EsmondAPI.tileToTimeRange("1d-10957") shouldEqual EsmondAPI.tileToTimeRange(10957)
      an[IllegalArgumentException] should be thrownBy EsmondAPI.tileToTimeRange("2d-5478")
    }
  }
}
