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

import nz.net.wand.streamevmon.{Configuration, TestBase}
import nz.net.wand.streamevmon.flink.FailingSource
import nz.net.wand.streamevmon.measurements.amp.ICMP
import nz.net.wand.streamevmon.measurements.MeasurementKeySelector

import java.time.Duration

import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

trait NoHarnessCheckpointingTestBase extends TestBase {
  val keySelector: MeasurementKeySelector[ICMP] = new MeasurementKeySelector[ICMP]

  def getEnv: StreamExecutionEnvironment = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val config = Configuration.get(Array())
    env.getConfig.setGlobalJobParameters(config)

    env.disableOperatorChaining

    env.setParallelism(1)

    env.enableCheckpointing(
      Duration.ofSeconds(1).toMillis,
      CheckpointingMode.EXACTLY_ONCE
    )

    env.setRestartStrategy(RestartStrategies.fixedDelayRestart(10, Time.seconds(1).toMilliseconds))

    env
  }

  def addFailingSource(env: StreamExecutionEnvironment): KeyedStream[ICMP, String] = {
    env.addSource(new FailingSource)
      .name("Failing Source")
      .uid("failing-source")
      .keyBy(keySelector)
  }
}
