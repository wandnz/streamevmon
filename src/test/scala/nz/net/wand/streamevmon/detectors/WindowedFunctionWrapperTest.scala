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

package nz.net.wand.streamevmon.detectors

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.amp.TraceroutePathlen
import nz.net.wand.streamevmon.measurements.traits.InfluxMeasurement
import nz.net.wand.streamevmon.measurements.MeasurementKeySelector
import nz.net.wand.streamevmon.test.TestBase

import java.time.Instant
import java.util.concurrent.TimeUnit

import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

class MyReallyFunOutOfOrderSourceFunction extends SourceFunction[InfluxMeasurement] with Serializable {
  def collect(ctx: SourceFunction.SourceContext[InfluxMeasurement], id: Int): Unit = {
    ctx.collectWithTimestamp(
      TraceroutePathlen(
        ":)",
        Some(1),
        Instant.ofEpochMilli(1000000000000L + TimeUnit.SECONDS.toMillis(id))
      ),
      1000000000000L + TimeUnit.SECONDS.toMillis(id)
    )
  }

  override def run(ctx: SourceFunction.SourceContext[InfluxMeasurement]): Unit = {
    for (x <- Range(0, 20)) {
      collect(ctx, x)
    }
    for (x <- Range(23, 25)) {
      collect(ctx, x)
    }
    for (x <- Range(20, 23)) {
      collect(ctx, x)
    }
    for (x <- Range(25, 50)) {
      collect(ctx, x)
    }
  }

  override def cancel(): Unit = {}
}

class AwesomeCheckOnlyIncreasingTimeFunction extends KeyedProcessFunction[String, InfluxMeasurement, Event]
                                                     with Serializable
                                                     with HasFlinkConfig {
  var lastTimeObserved = Instant.MIN

  override def processElement(value: InfluxMeasurement, ctx: KeyedProcessFunction[String, InfluxMeasurement, Event]#Context, out: Collector[Event]): Unit = {
    assert(value.time.compareTo(lastTimeObserved) >= 0)
    lastTimeObserved = value.time
  }

  override val flinkName = ""
  override val flinkUid = ""
  override val configKeyGroup = ""
}

class WindowedFunctionWrapperTest extends TestBase {
  "WindowedFunctionWrapper" should {
    "order items correctly within a window" in {
      val env = StreamExecutionEnvironment.getExecutionEnvironment

      env.getConfig.setGlobalJobParameters(Configuration.get(Array()))
      env.setParallelism(1)

      env
        .addSource(new MyReallyFunOutOfOrderSourceFunction)
        .setParallelism(1)
        .name("Fun Source")
        .keyBy(new MeasurementKeySelector[InfluxMeasurement])
        .window(TumblingEventTimeWindows.of(Time.seconds(18)))
        .process(
          new WindowedFunctionWrapper[InfluxMeasurement, TimeWindow](
            new AwesomeCheckOnlyIncreasingTimeFunction
          )
        )
        .name("Print Input Time")

      env.execute("Measurement subscription -> Print time")
    }
  }
}
