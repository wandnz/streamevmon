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

package nz.net.wand.streamevmon.checkpointing

import nz.net.wand.streamevmon.detectors.distdiff.{DistDiffDetector, WindowedDistDiffDetector}
import nz.net.wand.streamevmon.detectors.WindowedFunctionWrapper
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.MockSink
import nz.net.wand.streamevmon.measurements.amp.ICMP

import java.time.{Duration, Instant}

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.{GlobalWindow, TimeWindow}

class WindowedFunctionCheckpointingTest extends NoHarnessCheckpointingTestBase {
  "Windowed Functions" should {
    "recover from checkpoints" when {
      "wrapping a KeyedProcessFunction" in {
        val expected = Event(
          "distdiff_events",
          "3",
          100,
          Instant.ofEpochMilli(1000000001002L),
          Duration.ofNanos(182000000L),
          "Distribution of ICMP has changed. Mean has increased from 1000.0 to 63437.5",
          Map("windowed" -> "false")
        )

        val env = getEnv
        val src = addFailingSource(env)

        src.window(TumblingEventTimeWindows.of(Time.seconds(1)))
          .process(new WindowedFunctionWrapper[ICMP, TimeWindow](
            new DistDiffDetector[ICMP]
          ))
          .addSink(new MockSink())

        env.execute("WindowedFunctionWrapperTest (DistDiff)")

        MockSink.received should contain(expected)
      }

      "type is WindowedDistDiffDetector" in {

        val expected = Event(
          "distdiff_events",
          "3",
          100,
          Instant.ofEpochMilli(1000000000630L),
          Duration.ofNanos(200000000L),
          "Distribution of ICMP has changed. Mean has increased from 1000.0 to 63437.5",
          Map("windowed" -> "true")
        )

        val env = getEnv
        val src = addFailingSource(env)

        src.countWindow(40, 1)
          .process(new WindowedDistDiffDetector[ICMP, GlobalWindow])
          .addSink(new MockSink())

        env.execute("WindowedDistDiffDetectorTest")

        MockSink.received should contain(expected)
      }
    }
  }
}
