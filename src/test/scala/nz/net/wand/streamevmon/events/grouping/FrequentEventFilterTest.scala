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

import nz.net.wand.streamevmon.events.{Event, FrequentEventFilter}
import nz.net.wand.streamevmon.test.HarnessingTest

import java.time.{Duration, Instant}

import org.apache.flink.api.java.functions.KeySelector
import org.apache.flink.streaming.api.scala._

import scala.collection.JavaConverters._

class FrequentEventFilterTest extends HarnessingTest {
  val keySelector = new KeySelector[Event, String] {
    override def getKey(value: Event) = value.stream
  }

  "FrequentEventFilter" should {
    def eventWithTimestamp(stamp: Int): Event = Event(
      "test_events",
      "1",
      65,
      Instant.ofEpochSecond(stamp),
      Duration.ZERO,
      "test event",
      Map()
    )

    "recover from checkpoints" in {
      def func = new FrequentEventFilter()

      val f1 = func
      var harness = newHarness(f1, keySelector)
      harness.open()

      val sendThisManyEvents = 30

      def checkValidState(value: FrequentEventFilter): Unit = {
        withClue("One config should be disabled") {
          value.configEnabledMap("1").foreach { case (conf, time) =>
            if (conf.count <= sendThisManyEvents) {
              time shouldBe defined
            }
            else {
              time shouldNot be(defined)
            }
          }
        }

        withClue("all the measurements should be logged") {
          value.recentTimestamps("1") should have size sendThisManyEvents
        }
      }

      Range(0, sendThisManyEvents)
        .map(eventWithTimestamp)
        .foreach { e =>
          harness.processElement(e, e.time.toEpochMilli)
          currentTime = e.time.toEpochMilli
        }

      checkValidState(f1)

      val f2 = func

      harness = snapshotAndRestart(harness, f2, keySelector)
      checkValidState(f2)
    }

    "retain a list of the appropriate size with recent events' timestamps" in {
      val func = new FrequentEventFilter()
      val harness = newHarness(func, keySelector)
      harness.open()

      val longestInterval = func.longestInterval

      val startOfRange = 0
      val endOfRange = longestInterval.getSeconds.toInt * 2
      val step = endOfRange / 20
      val earliestExpectedTime = Instant.ofEpochSecond(endOfRange - longestInterval.getSeconds)

      Range(startOfRange, endOfRange + 1, step)
        .map(eventWithTimestamp)
        .foreach { ev =>
          harness.processElement(ev, ev.time.toEpochMilli)
        }

      func.recentTimestamps("1").foreach { item =>
        withClue(s"$item should not be before $earliestExpectedTime:") {
          item.isBefore(earliestExpectedTime) shouldBe false
        }
      }
    }

    "disable configs and replace events once enough events are provided" in {
      val func = new FrequentEventFilter()
      val harness = newHarness(func, keySelector)
      harness.open()

      val startOfRange = 0
      val endOfRange = 90
      val step = endOfRange / 20

      Range(startOfRange, endOfRange + 1, step)
        .map(eventWithTimestamp)
        .foreach { ev =>
          harness.processElement(ev, ev.time.toEpochMilli)
        }

      val numberOfUnfilteredEvents = func.configs.minBy(_.count).count
      val bulkEventConfigs = func.configs.filter(_.count <= 20)

      val outputs = harness.extractOutputValues.asScala.toList

      outputs.count(!_.eventType.startsWith("bulk_")) shouldBe numberOfUnfilteredEvents
      bulkEventConfigs.foreach { conf =>
        outputs
          .exists(ev =>
            ev.eventType.startsWith("bulk_") &&
              ev.description.contains(conf.name)
          ) shouldBe true
      }
    }

    "re-enable disabled configs after some time passes" in {
      val func = new FrequentEventFilter()
      val harness = newHarness(func, keySelector)
      harness.open()

      val firstEv = eventWithTimestamp(10)
      harness.processElement(firstEv, firstEv.time.toEpochMilli)
      func.configEnabledMap("1").foreach { case (k, _) =>
        func.configEnabledMap("1").put(k, Some(Instant.ofEpochSecond(10)))
      }

      func.configEnabledMap("1").values shouldNot contain(None)

      func.configs.map(_.cooldown).toList.sorted
        .foreach { cd =>
          val ev = eventWithTimestamp(15 + cd)
          harness.processElement(ev, ev.time.toEpochMilli)

          func.configEnabledMap("1")
            .filter(_._1.cooldown == cd)
            .foreach { case (_, v) =>
              v shouldNot be(defined)
            }
        }
    }

    "not re-enable disabled configs if they would be triggered the entire time" in {
      val func = new FrequentEventFilter()
      val harness = newHarness(func, keySelector)
      harness.open()

      val startOfRange = 0
      val endOfRange = 240
      val step = 5

      Range(startOfRange, endOfRange + 1, step)
        .map(eventWithTimestamp)
        .foreach { ev =>
          harness.processElement(ev, ev.time.toEpochMilli)
        }

      val numberOfUnfilteredEvents = func.configs.minBy(_.count).count
      val bulkEventConfigs = func.configs.filter(_.interval <= 240)

      val outputs = harness.extractOutputValues.asScala.toList

      outputs.count(!_.eventType.startsWith("bulk_")) shouldBe numberOfUnfilteredEvents
      bulkEventConfigs.foreach { conf =>
        withClue("Only one event should be output per config, but got") {
          outputs
            .count(ev =>
              ev.eventType.startsWith("bulk_") &&
                ev.description.contains(conf.name)
            ) shouldBe 1
        }
      }
    }
  }
}
