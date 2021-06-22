/* This file is part of streamevmon.
 *
 * Copyright (C) 2021  The University of Waikato, Hamilton, New Zealand
 *
 * Author: <unknown>
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

package nz.net.wand.streamevmon.events.grouping.graph.pruning

import nz.net.wand.streamevmon.test.TestBase

class MergeMapTest extends TestBase {
  "MergeMap" should {
    "allow simple get and retrieve" in {
      val map = new MergeMap[Int, String]()

      map.put(0, "0")
      map.put(1, "0")
      map.put(2, "2")

      map.get(0) shouldBe Some("0")
      map.get(1) shouldBe Some("0")
      map.get(2) shouldBe Some("2")

      map.put(0, "1")
      map.put(2, "2")

      map.get(0) shouldBe Some("1")
      // If two keys share the same value, and the value of one
      // of them changes, every key with the same value is updated
      map.get(1) shouldBe Some("1")
      map.get(2) shouldBe Some("2")
    }

    "treat merging correctly" in {
      val map = new MergeMap[Int, String]()

      map.put(0, "0")
      map.put(1, "1")
      map.put(2, "2")

      // Simple put/get combo, no merging
      map.get(0) shouldBe Some("0")
      map.get(1) shouldBe Some("1")
      map.get(2) shouldBe Some("2")

      map.merge(Seq(0, 1), 3, "3")

      // Merging existing keys with a new result key and value
      map.get(0) shouldBe Some("3")
      map.get(1) shouldBe Some("3")
      map.get(2) shouldBe Some("2")
      map.get(3) shouldBe Some("3")

      val map2 = new MergeMap[Int, String]()

      map2.put(0, "0")
      map2.put(1, "1")
      map2.put(2, "2")

      map2.get(0) shouldBe Some("0")
      map2.get(1) shouldBe Some("1")
      map2.get(2) shouldBe Some("2")

      // Merging existing keys with an existing result key and value
      map2.merge(Seq(0, 1), 1, "1")

      map2.get(0) shouldBe Some("1")
      map2.get(1) shouldBe Some("1")
      map2.get(2) shouldBe Some("2")

      val map3 = new MergeMap[Int, String]()

      map3.put(0, "0")
      map3.put(1, "1")
      map3.put(2, "2")

      map3.get(0) shouldBe Some("0")
      map3.get(1) shouldBe Some("1")
      map3.get(2) shouldBe Some("2")

      // Merging existing keys with an existing result key and a new value
      map3.merge(Seq(0, 1), 1, "3")

      map3.get(0) shouldBe Some("3")
      map3.get(1) shouldBe Some("3")
      map3.get(2) shouldBe Some("2")
    }

    "behave correctly when non-existent keys are supplied" in {
      val map = new MergeMap[Int, String]()

      map.put(0, "0")

      map.get(0) shouldBe Some("0")
      map.get(1) shouldBe None

      // Single non-existent key with new result key
      map.merge(Seq(1), 1, "1")
      map.get(1) shouldBe Some("1")

      // Multiple non-existent keys with new result key
      map.merge(Seq(2, 3), 2, "2")
      map.get(2) shouldBe Some("2")
      map.get(3) shouldBe Some("2")

      // Mix of existent and non-existent keys with new result key
      map.merge(Seq(3, 4), 4, "3")
      map.get(2) shouldBe Some("3")
      map.get(3) shouldBe Some("3")
      map.get(4) shouldBe Some("3")

      // Single non-existent key with existing result key
      map.merge(Seq(5), 1, "4")
      map.get(1) shouldBe Some("4")
      map.get(5) shouldBe Some("4")

      // Multiple non-existent keys with existing result key
      map.merge(Seq(6, 7), 1, "5")
      map.get(1) shouldBe Some("5")
      map.get(5) shouldBe Some("5")
      map.get(6) shouldBe Some("5")
      map.get(7) shouldBe Some("5")

      // Mix of existent and non-existent keys with existing result key
      map.merge(Seq(7, 8), 1, "6")
      map.get(1) shouldBe Some("6")
      map.get(5) shouldBe Some("6")
      map.get(6) shouldBe Some("6")
      map.get(7) shouldBe Some("6")
      map.get(7) shouldBe Some("6")
    }
  }
}
