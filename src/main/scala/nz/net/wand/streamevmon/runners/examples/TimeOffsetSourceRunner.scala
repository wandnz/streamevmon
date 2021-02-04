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
import nz.net.wand.streamevmon.flink.sources.{AmpMeasurementSourceFunction, PollingInfluxSourceFunction}
import nz.net.wand.streamevmon.measurements.traits.{InfluxMeasurement, InfluxMeasurementFactory}

import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Date

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._

import scala.collection.JavaConverters._

/** Shows the usage of the [[nz.net.wand.streamevmon.flink.sources.PollingInfluxSourceFunction PollingInfluxSourceFunction]]
  * to gather "live" data with a time offset. Outputs measurements arriving in
  * real-time with an [[nz.net.wand.streamevmon.flink.sources.AmpMeasurementSourceFunction AmpMeasurementSourceFunction]],
  * as well as measurements arriving a minute ago.
  */
object TimeOffsetSourceRunner {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    // Sets the subscription name for the AmpMeasurementSourceFunction.
    System.setProperty("source.influx.subscriptionName", "LiveSource")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))
    env.setParallelism(1)

    // Grab real-time data, and print its class and time.
    env
      .addSource(
        new AmpMeasurementSourceFunction(
          fetchHistory = Duration.ofMinutes(1)
        )
      )
      .name("Measurement Subscription")
      .map(i => s"${i.getClass.getSimpleName}(${new SimpleDateFormat("HH:mm:ss").format(Date.from(i.time))})")
      .print("Measurement Now")

    // Grab data from a minute ago, and print its class and time.
    env
      .addSource {
        new PollingInfluxSourceFunction[InfluxMeasurement](
          fetchHistory = Duration.ofMinutes(1),
          timeOffset = Duration.ofMinutes(1)
        ) {
          override protected def processLine(line: String): Option[InfluxMeasurement] = InfluxMeasurementFactory.createMeasurement(line)

          override val flinkName: String = "Offset 1 Minute AMP Measurement Source"
          override val flinkUid: String = "1-min-ago-amp-measurement-source"
        }
          // TODO: Check that we actually need this separate configuration.
          //  Since this is a polling function, it shouldn't subscribe and won't
          //  overwrite the subscription for the real-time function.
          .overrideConfig(
            env.getConfig.getGlobalJobParameters.asInstanceOf[ParameterTool].mergeWith(
              ParameterTool.fromMap(Map(
                "source.influx.subscriptionName" -> "TimeOffsetSource"
              ).asJava)
            )
          )
      }
      .name("Measurement Subscription")
      .map(i => s"${i.getClass.getSimpleName}(${new SimpleDateFormat("HH:mm:ss").format(Date.from(i.time))})")
      .print("Measurement Then")

    env.execute("Real-time and one-minute-ago printer")
  }
}
