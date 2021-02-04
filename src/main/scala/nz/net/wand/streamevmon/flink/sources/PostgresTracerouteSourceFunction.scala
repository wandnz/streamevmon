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

package nz.net.wand.streamevmon.flink.sources

import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.connectors.postgres.PostgresConnection
import nz.net.wand.streamevmon.measurements.amp.{Traceroute, TracerouteMeta}

import java.time.{Duration, Instant}

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}

/** Retrieves new Traceroute measurements from PostgreSQL in a polling fashion.
  *
  * Each time a new query is constructed, this source will refresh its list of
  * relevant streams. This allows any newly created streams to be picked up.
  *
  * ==Configuration==
  *
  * See [[nz.net.wand.streamevmon.connectors.postgres.PostgresConnection PostgresConnection]]
  * for configuration details.
  *
  * Additionally, this SourceFunction will sleep for the time in seconds
  * specified by `source.postgres.tracerouteRefreshDelay` after each query.
  *
  * Given a `fetchHistory` of one hour, and a `tracerouteRefreshDelay` of five
  * minutes, the source will use the following procedure:
  *
  * - Output the last hour's worth of measurements all at once
  * - Wait for five minutes
  * - Output new measurements that arrived in the last five minutes
  * - Wait for five minutes
  * - etc...
  */
class PostgresTracerouteSourceFunction(
  fetchHistory: Duration = Duration.ZERO
)
  extends RichSourceFunction[Traceroute]
          with HasFlinkConfig
          with CheckpointedFunction
          with Logging {

  override val flinkName: String = "PostgreSQL Measurement Source"
  override val flinkUid: String = "postgres-measurement-source"
  override val configKeyGroup: String = "postgres"

  @volatile
  @transient protected var isRunning = false

  var lastMeasurementTime: Instant = Instant.now().minus(fetchHistory)

  def refreshUsedStreams(
    pgCon         : PostgresConnection,
    ampletToAmplet: Boolean = true
  ): Iterable[TracerouteMeta] = {
    logger.info("Refreshing traceroute stream library...")
    pgCon.getAllTracerouteMeta match {
      case Some(value) =>
        // We construct a list of potential filters and whether they're enabled.
        // Each filter corresponds to a boolean argument that was passed to
        // this function.
        Seq[(Boolean, TracerouteMeta => Boolean)](
          // Only retains streams that have a known amplet as a destination
          (ampletToAmplet, _ => true)
          // Next, we apply all the filters to the list of all the streams.
          // The order doesn't matter, but this is easier to write than .fold()
        ).foldLeft(value) {
          // If the filter was enabled by the corresponding function argument...
          case (metas, (enabled, func)) => if (enabled) {
            // apply it
            metas.filter(func)
          }
          else {
            // otherwise do nothing
            metas
          }
        }
      case None =>
        logger.info("Error getting TracerouteMeta entries.")
        None
    }
  }

  override def run(ctx: SourceFunction.SourceContext[Traceroute]): Unit = {
    val params = configWithOverride(getRuntimeContext)
    val pgCon = PostgresConnection(params, configKeyGroup)
    val refreshDelay = params.getLong(s"source.$configKeyGroup.tracerouteRefreshDelay")

    if (getRuntimeContext.getNumberOfParallelSubtasks > 1) {
      throw new IllegalStateException("Parallelism for this SourceFunction must be 1.")
    }

    isRunning = true

    while (isRunning) {
      val usedStreams = refreshUsedStreams(pgCon)
      val now = Instant.now()
      logger.info(s"Getting traceroute measurements for ${usedStreams.size} streams between $lastMeasurementTime and $now...")
      usedStreams.flatMap { meta =>
        pgCon.getTracerouteData(meta.stream, lastMeasurementTime, now)
      }
        .flatten
        .foreach { meas =>
          if (meas.timestamp > lastMeasurementTime.getEpochSecond) {
            lastMeasurementTime = meas.time
          }
          ctx.collect(meas)
        }
      Thread.sleep(Duration.ofSeconds(refreshDelay).toMillis)
    }
    pgCon.closeSession()
  }

  override def cancel(): Unit = {
    logger.info("Stopping listener...")
    isRunning = false
  }

  // We only put the last measurement time in our checkpoint state. The rest
  // is transient and should get reconstructed at next startup.

  private var checkpointState: ListState[Instant] = _

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    checkpointState.clear()
    checkpointState.add(lastMeasurementTime)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    checkpointState = context
      .getOperatorStateStore
      .getListState(new ListStateDescriptor[Instant]("lastMeasurementTime", classOf[Instant]))

    if (context.isRestored) {
      lastMeasurementTime = checkpointState.get().iterator().next()
    }
  }
}
