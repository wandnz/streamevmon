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
import nz.net.wand.streamevmon.flink.sources.Amp2SourceFunction

import java.text.DecimalFormat
import java.time.Duration
import java.util.concurrent.TimeUnit

import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.CheckpointingMode

import scala.collection.mutable

object Amp2SourcePrinter {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.enableCheckpointing(10000, CheckpointingMode.EXACTLY_ONCE)
    env.setRestartStrategy(RestartStrategies.noRestart())

    env.setParallelism(1)

    System.setProperty("source.influx.subscriptionName", "Amp2SourcePrinter")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    val source = {
      val func = new Amp2SourceFunction(fetchHistory = Duration.ofDays(365))
      env
        .addSource(func)
        .name(func.flinkName)
        .uid(func.flinkUid)
    }

    val typeMap: mutable.Map[String, Int] = mutable.Map()
    var lastTime = System.nanoTime()
    val interval = 10000
    val format = new DecimalFormat("#####.##")

    source
      .map { meas =>
        if (typeMap.contains(meas.getClass.getSimpleName)) {
          typeMap.put(meas.getClass.getSimpleName, typeMap(meas.getClass.getSimpleName) + 1)
        }
        else {
          typeMap.put(meas.getClass.getSimpleName, 1)
        }

        if (typeMap.values.sum % interval == 0) {
          val time = System.nanoTime()
          println(
            s"${typeMap.values.sum} items, " +
              s"${TimeUnit.NANOSECONDS.toMillis(time - lastTime)}ms, " +
              s"${format.format((interval.toDouble * 1E9) / (time - lastTime))} item/s, " +
              s"${typeMap.toList.sortBy(_._1)}"
          )
          lastTime = System.nanoTime()
        }
      }

    env.execute("Amp2 Source Printer")
  }
}
