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

package nz.net.wand.streamevmon.runners.examples

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.flink.sources.LatencyTSAmpFileInputFormat
import nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP
import nz.net.wand.streamevmon.measurements.traits.{HasDefault, Measurement}

import java.time.Instant

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessAllWindowFunction
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import scala.compat.Platform.EOL

/** Gathers some statistical metrics from a measurement stream which implements
  * [[nz.net.wand.streamevmon.measurements.traits.HasDefault HasDefault]]. It creates
  * a rolling window and gathers metrics from that, then prints the results.
  *
  * @see [[http://commons.apache.org/proper/commons-math/javadocs/api-3.3/org/apache/commons/math3/stat/descriptive/SummaryStatistics.html Apache Commons Math SummaryStatistics]]
  *      for an alternative way to gather statistics.
  */
object MetricGatherer {

  case class Metrics[T <: Measurement with HasDefault](elements: Iterable[T]) {
    private lazy val elementsAsDoubles = elements.filter(_.defaultValue.isDefined).map(_.defaultValue.get)
    private lazy val elementsAsDoublesSorted = elementsAsDoubles.toSeq.sorted

    lazy val startTime: Instant = elements.minBy(_.time).time
    lazy val endTime: Instant = elements.maxBy(_.time).time

    lazy val mean: Double = elementsAsDoubles.sum / elementsAsDoubles.size
    lazy val median: Double = elementsAsDoublesSorted(elementsAsDoublesSorted.size / 2)

    def percentile(percent: Int): Double = {
      elementsAsDoublesSorted(math.ceil((elementsAsDoublesSorted.size - 1) * (percent / 100.0)).toInt)
    }

    override def toString: String = s"Metrics between $startTime and $endTime:" + EOL +
      s"Mean: $mean" + EOL +
      s"Median: $median" + EOL +
      s"75%: ${percentile(75)}" + EOL +
      s"Elements (${elements.size}): $elementsAsDoublesSorted"
  }

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    env.getConfig.setGlobalJobParameters(Configuration.get(args))
    env.disableOperatorChaining()

    env
      .readFile(new LatencyTSAmpFileInputFormat, "/usr/share/flink/data/latency-ts-i/ampicmp/series/waikato-xero-ipv4.series")
      .setParallelism(1)
      .assignAscendingTimestamps(_.time.toEpochMilli)
      .keyBy(_.stream)
      .windowAll(TumblingEventTimeWindows.of(Time.hours(1)))
      .process {
        new ProcessAllWindowFunction[LatencyTSAmpICMP, Metrics[LatencyTSAmpICMP], TimeWindow] {
          override def process(
            context: Context,
            elements: Iterable[LatencyTSAmpICMP],
            out    : Collector[Metrics[LatencyTSAmpICMP]]
          ): Unit = {
            out.collect(Metrics(elements))
          }
        }
      }
      .print("Metrics")

    env.execute()
  }
}
