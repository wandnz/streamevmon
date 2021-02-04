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

package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.{Caching, Lazy}
import nz.net.wand.streamevmon.measurements.traits.Measurement
import nz.net.wand.streamevmon.measurements.MeasurementKeySelector

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.Window

/** Contains a type-filtered stream, and allows further modifying it to filter
  * lossy measurements and get the keyed variant. Everything is lazy, so streams
  * are only constructed and put onto the execution graph if they need to be.
  */
case class TypedStreams(
  typedStream: Lazy[DataStream[Measurement]]
) extends Caching {

  useInMemoryCache()

  lazy val notLossy: DataStream[Measurement] = typedStream.get
    .filter(!_.isLossy)
    .name("Is not lossy?")
  lazy val keyedStream: KeyedStream[Measurement, String] = typedStream.get
    .keyBy(new MeasurementKeySelector[Measurement])
  lazy val notLossyKeyedStream: KeyedStream[Measurement, String] = notLossy
    .keyBy(new MeasurementKeySelector[Measurement])

  def getWindowedStream(
    sourceName        : String,
    notLossy          : Boolean,
    windowType        : StreamWindowType.Value,
    timeWindowDuration: Time,
    countWindowSize   : Long,
    countWindowSlide  : Long
  ): WindowedStream[Measurement, String, Window] = {
    getWithCache(
      s"windowed-stream:$sourceName-$notLossy-$windowType-$timeWindowDuration-$countWindowSize-$countWindowSlide",
      ttl = None,
      method = {
        (windowType, notLossy) match {
          case (_: StreamWindowType.TimeWithOverrides, true) =>
            notLossyKeyedStream.window(TumblingEventTimeWindows.of(timeWindowDuration))
          case (_: StreamWindowType.TimeWithOverrides, false) =>
            keyedStream.window(TumblingEventTimeWindows.of(timeWindowDuration))
          case (_: StreamWindowType.CountWithOverrides, true) =>
            notLossyKeyedStream.countWindow(countWindowSize, countWindowSlide)
          case (_: StreamWindowType.CountWithOverrides, false) =>
            keyedStream.countWindow(countWindowSize, countWindowSlide)
          case t => throw new IllegalArgumentException(s"Unrecognised StreamWindowType $t")
        }
      }
    ).asInstanceOf[WindowedStream[Measurement, String, Window]]
  }
}
