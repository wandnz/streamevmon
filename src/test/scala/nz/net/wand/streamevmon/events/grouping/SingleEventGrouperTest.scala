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

package nz.net.wand.streamevmon.events.grouping

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.test.HarnessingTest

import java.time.{Duration, Instant}

import scala.collection.JavaConverters._

class SingleEventGrouperTest extends HarnessingTest {
  "SingleEventGrouper" should {
    val inputs = Seq(
      (Event(
        "test_event",
        "1",
        50,
        Instant.ofEpochMilli(10000L),
        Duration.ofSeconds(5),
        "Test event",
        Map()
      ), 5000L),
      (Event(
        "test_event",
        "1",
        100,
        Instant.ofEpochMilli(20000L),
        Duration.ofSeconds(10),
        "Test event in same stream",
        Map("data" -> "more_data")
      ), 10000L),
      (Event(
        "test_event",
        "2",
        75,
        Instant.ofEpochMilli(30000L),
        Duration.ofSeconds(15),
        "Test event in different stream",
        Map()
      ), 15000L),
    )

    val outputs = Seq(
      EventGroup(
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
      ),
      EventGroup(
        Instant.ofEpochMilli(10000L),
        Some(Instant.ofEpochMilli(10000L)),
        Seq(Event(
          "test_event",
          "1",
          100,
          Instant.ofEpochMilli(20000L),
          Duration.ofSeconds(10),
          "Test event in same stream",
          Map("data" -> "more_data")
        ))
      ),
      EventGroup(
        Instant.ofEpochMilli(15000L),
        Some(Instant.ofEpochMilli(15000L)),
        Seq(Event(
          "test_event",
          "2",
          75,
          Instant.ofEpochMilli(30000L),
          Duration.ofSeconds(15),
          "Test event in different stream",
          Map()
        ))
      ),
    )

    "place events into individual event groups" in {
      val func = new SingleEventGrouper()
      val harness = newHarness(func)
      harness.open()

      inputs.foreach(e => harness.processElement(e._1, e._2))
      harness.extractOutputValues.asScala shouldBe outputs
    }
  }
}
