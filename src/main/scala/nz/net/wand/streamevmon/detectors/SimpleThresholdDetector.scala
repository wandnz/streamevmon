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

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.amp.RichICMP
import nz.net.wand.streamevmon.measurements.traits.Measurement

import java.time.Duration

import org.apache.flink.streaming.api.scala.function.ProcessAllWindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

/** Very basic example of threshold detection.
  *
  * Examines [[nz.net.wand.streamevmon.measurements.amp.RichICMP RichICMP]]
  * objects, and emits events with a constant severity if the median value is
  * greater than the specified value (default 1000).
  *
  * @tparam T This class can accept any type of Measurement, but only provides
  *           output if the measurement is a RichICMP.
  */
class SimpleThresholdDetector[T <: Measurement](threshold: Int = 1000)
    extends ProcessAllWindowFunction[T, Event, TimeWindow] {

  override def process(context: Context, elements: Iterable[T], out: Collector[Event]): Unit = {
    elements
      .filter(_.isInstanceOf[RichICMP])
      .map(_.asInstanceOf[RichICMP])
      .filter(_.median.getOrElse(Int.MinValue) > threshold)
      .foreach { m =>
        out.collect(
          new Event(
            "threshold_events",
            m.stream,
            severity = 10,
            m.time,
            Duration.ZERO,
            s"Median latency was over $threshold",
            Map()
          )
        )
      }
  }
}
