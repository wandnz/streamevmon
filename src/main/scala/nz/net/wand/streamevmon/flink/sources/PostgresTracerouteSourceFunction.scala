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
import nz.net.wand.streamevmon.flink.sources.PostgresTracerouteSourceFunction.ampletToAmplet
import nz.net.wand.streamevmon.measurements.amp.{Traceroute, TracerouteMeta}

import java.time.{Duration, Instant}

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}

import scala.collection.mutable
import scala.collection.JavaConverters._

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
  * Additionally, this SourceFunction uses the `source.postgres.traceroute` key
  * group for extra config.
  *
  * - `refreshDelay`: After each query, the SourceFunction will sleep for this
  * amount of time in seconds. Default 600.
  * - The `filters` subgroup allows the user to enable methods of reducing the
  * number of traceroute measurement streams to be considered. These filters
  * can drastically reduce memory usage and runtime for all operators that use
  * the resultant Traceroute measurements, including this one.
  *   - `ampletToAmplet`: If true, only streams containing traces between two
  *     amplets will be considered. For example, a trace from an amplet to
  *     google.com will be discarded.
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

  val lastMeasurementTimes: mutable.Map[Int, Instant] = mutable.Map()

  def refreshUsedStreams(
    pgCon          : PostgresConnection,
    filterFunctions: Iterable[Iterable[TracerouteMeta] => Set[TracerouteMeta]]
  ): Iterable[TracerouteMeta] = {
    logger.info("Refreshing traceroute stream library...")
    pgCon.getAllTracerouteMeta match {
      case Some(value) =>
        // We apply all the filters to the list of all the streams.
        // Each of them needs to know the full list of streams.
        if (filterFunctions.nonEmpty) {
          filterFunctions
            .map(_ (value))
            .reduce[Set[TracerouteMeta]] { case (filtered1, filtered2) =>
              filtered1.intersect(filtered2)
            }
        }
        else {
          value
        }
      case None =>
        logger.info("Error getting TracerouteMeta entries.")
        None
    }
  }

  def getFilterFunctions(params: ParameterTool): Iterable[Iterable[TracerouteMeta] => Set[TracerouteMeta]] = {
    Seq(
      ("ampletToAmplet", ampletToAmplet),
    ).flatMap { case (funcName, func) =>
      if (params.getBoolean(s"source.$configKeyGroup.traceroute.filters.$funcName")) {
        Some(func)
      }
      else {
        None
      }
    }
  }

  override def run(ctx: SourceFunction.SourceContext[Traceroute]): Unit = {
    val params = configWithOverride(getRuntimeContext)
    val pgCon = PostgresConnection(params, configKeyGroup)
    val refreshDelay = params.getLong(s"source.$configKeyGroup.traceroute.refreshDelay")

    if (getRuntimeContext.getNumberOfParallelSubtasks > 1) {
      throw new IllegalStateException("Parallelism for this SourceFunction must be 1.")
    }

    val filters = getFilterFunctions(params)

    isRunning = true

    while (isRunning) {
      val usedStreams = refreshUsedStreams(pgCon, filters)
      val now = Instant.now()
      logger.info(s"Getting traceroute measurements for ${usedStreams.size} streams until $now...")
      usedStreams.flatMap { meta =>
        val oldestTime = lastMeasurementTimes.get(meta.stream) match {
          case Some(value) => value
          case None =>
            val oldTime = now.minus(fetchHistory)
            lastMeasurementTimes.put(meta.stream, oldTime)
            oldTime
        }
        pgCon.getTracerouteData(meta.stream, oldestTime, now)
      }
        .flatten
        .foreach { meas =>
          if (meas.timestamp > lastMeasurementTimes(meas.stream.toInt).getEpochSecond) {
            lastMeasurementTimes.put(meas.stream.toInt, meas.time)
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

  private var checkpointState: ListState[(Int, Instant)] = _

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    checkpointState.clear()
    checkpointState.addAll(lastMeasurementTimes.toSeq.asJava)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    checkpointState = context
      .getOperatorStateStore
      .getListState(new ListStateDescriptor[(Int, Instant)]("lastMeasurementTimes", classOf[(Int, Instant)]))

    if (context.isRestored) {
      checkpointState.get.forEach(item => lastMeasurementTimes.put(item._1, item._2))
    }
  }
}

/** This includes the various filter functions for refreshUsedStreams, which
  * get configured by the `filters` key subgroup.
  */
object PostgresTracerouteSourceFunction {
  val ampletToAmplet: Iterable[TracerouteMeta] => Set[TracerouteMeta] = { metas =>
    val knownAmplets = metas.map(_.source).toSet
    metas.filterNot(meta => knownAmplets.contains(meta.destination)).toSet
  }
}
