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

package nz.net.wand.streamevmon.events.grouping.graph.grouping

class DistanceBetweenStreamsTest extends StreamDistanceTests {
  "DistanceBetweenStreams" should {
    "give distance estimates that are useful relative to one another" in {
      val g = makeGraph()
      val baseMeta = metaOf(baseSource, baseDest)
      val nearMeta = metaOf(nearSource, nearDest)
      val farMeta = metaOf(farSource, farDest)

      // Reversed pairs should have the same distance
      Seq(
        (baseMeta, nearMeta),
        (baseMeta, farMeta),
        (nearMeta, farMeta)
      ).foreach { case (i, j) =>
        DistanceBetweenStreams.get(g, i, j) shouldBe DistanceBetweenStreams.get(g, j, i)
      }

      // The distance from one stream to another should be 0
      DistanceBetweenStreams.get(g, baseMeta, baseMeta) shouldBe StreamDistance.ZERO
      DistanceBetweenStreams.get(g, nearMeta, nearMeta) shouldBe StreamDistance.ZERO
      DistanceBetweenStreams.get(g, farMeta, farMeta) shouldBe StreamDistance.ZERO

      // Streams that are further away should have a greater distance
      DistanceBetweenStreams.get(g, baseMeta, nearMeta) shouldBe <(DistanceBetweenStreams.get(g, baseMeta, farMeta))
    }
  }
}
