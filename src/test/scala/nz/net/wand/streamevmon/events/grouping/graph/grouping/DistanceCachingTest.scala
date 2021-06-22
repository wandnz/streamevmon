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

import nz.net.wand.streamevmon.test.HarnessingTest

import java.time.Instant

class DistanceCachingTest extends StreamDistanceTests with HarnessingTest {
  "StreamDistanceCache" should {
    "calculate distances to all known streams when a new one is added" in {
      val g = makeGraph()
      val c = new StreamDistanceCache

      c.receiveStream(g, metaOf(baseSource, baseDest), Instant.ofEpochSecond(currentTime))
      currentTime += 1
      c.knownDistances shouldBe empty

      c.receiveStream(g, metaOf(farSource, farDest), Instant.ofEpochSecond(currentTime))
      currentTime += 1
      c.knownDistances should have size 1
      c.knownDistances.head._1 shouldBe new c.PairT(
        metaOf(baseSource, baseDest), metaOf(farSource, farDest)
      )

      c.receiveStream(g, metaOf(nearSource, nearDest), Instant.ofEpochSecond(currentTime))
      currentTime += 1
      c.knownDistances should have size 3
      c.knownDistances.keySet shouldBe Set(
        new c.PairT(metaOf(baseSource, baseDest), metaOf(nearSource, nearDest)),
        new c.PairT(metaOf(baseSource, baseDest), metaOf(farSource, farDest)),
        new c.PairT(metaOf(nearSource, nearDest), metaOf(farSource, farDest)),
      )
    }

    "update last-seen-time" in {
      val g = makeGraph()
      val c = new StreamDistanceCache

      Range(1000, 2000).foreach { time =>
        c.receiveStream(g, metaOf(baseSource, baseDest), Instant.ofEpochSecond(time))
        c.lastSeenTimes.get(metaOf(baseSource, baseDest)) shouldBe Some(Instant.ofEpochSecond(time))
      }
    }

    "recalculate distances properly" in {
      val g = makeGraph()
      val c = new StreamDistanceCache
      c.receiveStream(g, metaOf(baseSource, baseDest), Instant.ofEpochSecond(currentTime))
      currentTime += 1
      c.receiveStream(g, metaOf(nearSource, nearDest), Instant.ofEpochSecond(currentTime))
      currentTime += 1
      c.receiveStream(g, metaOf(farSource, farDest), Instant.ofEpochSecond(currentTime))
      currentTime += 1

      val correctDistances = c.knownDistances.map(identity)
      // rather than changing the graph, i'm just gonna muck with the knownDistances storage
      c.knownDistances.foreach { case (pair, _) =>
        c.knownDistances.put(pair, StreamDistance.ZERO)
      }

      c.recalculateAllDistances(g)
      c.knownDistances shouldBe correctDistances
    }

    "clear entries" in {
      val g = makeGraph()
      val c = new StreamDistanceCache
      c.receiveStream(g, metaOf(baseSource, baseDest), Instant.ofEpochSecond(currentTime))
      currentTime += 1
      c.receiveStream(g, metaOf(nearSource, nearDest), Instant.ofEpochSecond(currentTime))
      currentTime += 1
      c.receiveStream(g, metaOf(farSource, farDest), Instant.ofEpochSecond(currentTime))
      currentTime += 1

      c.clear()

      c.lastSeenTimes shouldBe empty
      c.knownDistances shouldBe empty
    }

    "get distance entries given one key" in {
      val g = makeGraph()
      val c = new StreamDistanceCache
      c.receiveStream(g, metaOf(baseSource, baseDest), Instant.ofEpochSecond(currentTime))
      currentTime += 1
      c.receiveStream(g, metaOf(nearSource, nearDest), Instant.ofEpochSecond(currentTime))
      currentTime += 1
      c.receiveStream(g, metaOf(farSource, farDest), Instant.ofEpochSecond(currentTime))
      currentTime += 1

      val result = c.getDistancesFor(metaOf(baseSource, baseDest))
      result should have size 2
      result.keys should contain(metaOf(nearSource, nearDest))
      result.keys should contain(metaOf(farSource, farDest))

      c.getDistancesFor(metaOf(baseSource, farDest)) shouldBe empty
    }

    "get a distance entry given two keys" in {
      val g = makeGraph()
      val c = new StreamDistanceCache
      c.receiveStream(g, metaOf(baseSource, baseDest), Instant.ofEpochSecond(currentTime))
      currentTime += 1
      c.receiveStream(g, metaOf(nearSource, nearDest), Instant.ofEpochSecond(currentTime))
      currentTime += 1
      c.receiveStream(g, metaOf(farSource, farDest), Instant.ofEpochSecond(currentTime))
      currentTime += 1

      // valid
      c.getDistanceBetween(metaOf(baseSource, baseDest), metaOf(nearSource, nearDest)) should not be None
      // swapped
      c.getDistanceBetween(metaOf(nearSource, nearDest), metaOf(baseSource, baseDest)) should not be None
      // one invalid
      c.getDistanceBetween(metaOf(baseSource, baseDest), metaOf(baseSource, farDest)) shouldBe None
      // one invalid, swapped
      c.getDistanceBetween(metaOf(baseSource, farDest), metaOf(baseSource, baseDest)) shouldBe None
      // two invalid
      c.getDistanceBetween(metaOf(baseSource, nearDest), metaOf(baseSource, farDest)) shouldBe None
      // same
      c.getDistanceBetween(metaOf(baseSource, baseDest), metaOf(baseSource, baseDest)) shouldBe Some(StreamDistance.ZERO)
    }
  }
}
