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
import nz.net.wand.streamevmon.flink.sources.AmpMeasurementSourceFunction

import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Date

import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.scala._

/** Most basic example of using an [[nz.net.wand.streamevmon.flink.sources.InfluxSourceFunction InfluxSourceFunction]],
  * in this case an [[nz.net.wand.streamevmon.flink.sources.AmpMeasurementSourceFunction AmpMeasurementSourceFunction]].
  *
  * Requires `source.influx.(amp.)?serverName` to be set.
  *
  * This just prints the type of measurement that was received and its time.
  */
object InfluxSourceReceiver {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.enableCheckpointing(10000, CheckpointingMode.EXACTLY_ONCE)
    env.setRestartStrategy(RestartStrategies.noRestart())

    System.setProperty("source.influx.subscriptionName", "InfluxSourceReceiver")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env
      .addSource(new AmpMeasurementSourceFunction(fetchHistory = Duration.ofHours(1)))
      .name("Measurement Subscription")
      .map(i =>
        s"${i.getClass.getSimpleName}(${new SimpleDateFormat("HH:mm:ss").format(Date.from(i.time))})")
      .print("Measurement")

    env.execute("InfluxDB subscription printer")
  }
}
