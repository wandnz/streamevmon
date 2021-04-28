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
import nz.net.wand.streamevmon.events.grouping.graph.building.FlinkHelpers
import nz.net.wand.streamevmon.events.grouping.graph.GraphDotExporter
import nz.net.wand.streamevmon.flink.sources.PostgresTracerouteSourceFunction

import java.time.Duration

import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.CheckpointingMode

object GraphBuilder {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.enableCheckpointing(Duration.ofSeconds(60).toMillis, CheckpointingMode.EXACTLY_ONCE)
    env.getCheckpointConfig.setCheckpointTimeout(Duration.ofSeconds(600).toMillis)
    env.getCheckpointConfig.setMinPauseBetweenCheckpoints(Duration.ofSeconds(10).toMillis)
    env.getCheckpointConfig.setMaxConcurrentCheckpoints(1)

    env.setRestartStrategy(RestartStrategies.noRestart())
    env.setParallelism(1)
    env.disableOperatorChaining()

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    val traceroutes = {
      val pgSourceFunction = new PostgresTracerouteSourceFunction(
        fetchHistory = Duration.ofDays(1)
      )
      env
        .addSource(pgSourceFunction)
        .name(pgSourceFunction.flinkName)
        .uid(pgSourceFunction.flinkUid)
    }

    val graphStream = FlinkHelpers.tracerouteToGraph(traceroutes)

    val exporter = {
      val function = new GraphDotExporter()
      graphStream
        .process(function)
        .name(function.flinkName)
        .uid(function.flinkUid)
    }

    println(env.getExecutionPlan.replace("\n", ""))

    exporter.addSink(_ => Unit)

    env.execute()
  }
}
