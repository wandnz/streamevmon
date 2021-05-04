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

package nz.net.wand.streamevmon.events.grouping.time

import nz.net.wand.streamevmon.events.grouping.EventGroup
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.test.HarnessingTest

import java.time.{Duration, Instant}

import org.apache.flink.api.scala._

class TemporalEventGrouperTest extends HarnessingTest {
  "TemporalEventGrouper" should {
    def getHarness = {
      val func = new TemporalEventGrouper
      (
        func,
        newHarness(
          func,
          (value: EventGroup) => value.events.headOption.map(_.stream).getOrElse("")
        )
      )
    }

    val firstGroup = EventGroup(
      Instant.ofEpochMilli(5000L),
      Some(Instant.ofEpochMilli(5000L)),
      Seq(Event(
        "test_event",
        "1",
        50,
        Instant.ofEpochMilli(10000L),
        Duration.ofSeconds(5),
        "Test event",
        Map()
      ))
    )

    val shortlyAfter = EventGroup(
      Instant.ofEpochMilli(5100L),
      Some(Instant.ofEpochMilli(5100L)),
      Seq(Event(
        "test_event",
        "1",
        55,
        Instant.ofEpochMilli(10100L),
        Duration.ofSeconds(5),
        "Test event shortly after the first one",
        Map()
      ))
    )

    val shortlyAfterGroup = EventGroup(
      Instant.ofEpochMilli(5000L),
      Some(Instant.ofEpochMilli(5100L)),
      Seq(
        firstGroup.events.head,
        shortlyAfter.events.head
      )
    )

    val kindaLongAfter = EventGroup(
      Instant.ofEpochMilli(3500L),
      Some(Instant.ofEpochMilli(3500L)),
      Seq(Event(
        "test_event",
        "1",
        65,
        Instant.ofEpochMilli(4000L),
        Duration.ofSeconds(5),
        "Test event kinda long after the first one",
        Map()
      ))
    )

    val kindaLongAfterGroups = Seq(firstGroup, kindaLongAfter)

    val aVeryLongTimeAfter = EventGroup(
      Instant.ofEpochMilli(995000L),
      Some(Instant.ofEpochMilli(995000L)),
      Seq(Event(
        "test_event",
        "1",
        50,
        Instant.ofEpochMilli(1000000L),
        Duration.ofSeconds(5),
        "Test event that happens way after the original one",
        Map()
      ))
    )

    "place nearby events in the same group" in {
      val (_, harness) = getHarness
      harness.open()

      harness.processElement(firstGroup, firstGroup.startTime.toEpochMilli)
      harness.processElement(shortlyAfter, shortlyAfter.startTime.toEpochMilli)
      harness.processElement(aVeryLongTimeAfter, aVeryLongTimeAfter.startTime.toEpochMilli)

      harness.extractOutputValues should contain(shortlyAfterGroup)
    }

    "send an event group if no event is received for a while" in {
      val (_, harness) = getHarness
      harness.open()

      harness.processElement(firstGroup, firstGroup.startTime.toEpochMilli)
      harness.processWatermark(aVeryLongTimeAfter.startTime.toEpochMilli)

      harness.extractOutputValues should contain(firstGroup)
    }

    "make separate groups" when {
      "events are further apart than the maximumEventLength" in {
        val (_, harness) = getHarness
        harness.open()

        harness.processElement(firstGroup, firstGroup.startTime.toEpochMilli)
        harness.processElement(shortlyAfter, shortlyAfter.startTime.toEpochMilli)
        harness.processElement(aVeryLongTimeAfter, aVeryLongTimeAfter.startTime.toEpochMilli)
        harness.processWatermark(aVeryLongTimeAfter.startTime.plus(Duration.ofDays(100)).toEpochMilli)

        harness.extractOutputValues should contain(shortlyAfterGroup)
        harness.extractOutputValues should contain(aVeryLongTimeAfter)
      }
    }

    "restore from checkpoints" in {
      val (_, harness) = getHarness
      harness.open()

      harness.processElement(firstGroup, firstGroup.startTime.toEpochMilli)

      val snapshot = harness.snapshot(1, 6000L)
      harness.close()

      val (_, nextHarness) = getHarness
      nextHarness.setup()
      nextHarness.initializeState(snapshot)
      nextHarness.open()
      nextHarness.processElement(aVeryLongTimeAfter, aVeryLongTimeAfter.startTime.toEpochMilli)

      nextHarness.extractOutputValues should contain(firstGroup)
    }
  }
}
