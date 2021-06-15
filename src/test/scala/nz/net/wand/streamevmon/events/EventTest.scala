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

package nz.net.wand.streamevmon.events

import nz.net.wand.streamevmon.test.{SeedData, TestBase}

class EventTest extends TestBase {

  "Events should become strings appropriately" when {
    "some tags are present" in {
      SeedData.event.withTags.toString shouldBe SeedData.event.withTagsAsString
      SeedData.event.withTags.toLineProtocol shouldBe SeedData.event.withTagsAsLineProtocol
    }

    "no tags are present" in {
      SeedData.event.withoutTags.toString shouldBe SeedData.event.withoutTagsAsString
      SeedData.event.withoutTags.toLineProtocol shouldBe SeedData.event.withoutTagsAsLineProtocol
    }

    "an amp2-style stream ID is present" in {
      SeedData.event.amp2Event.toLineProtocol shouldBe SeedData.event.amp2EventAsLineProtocol
    }
  }

  "Events should become CSVs properly" in {
    SeedData.event.withTags.toCsvFormat shouldBe SeedData.event.withTagsAsCsv
    SeedData.event.withoutTags.toCsvFormat shouldBe SeedData.event.withoutTagsAsCsv
  }
}
