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

package nz.net.wand.streamevmon.events.grouping.graph.building

import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.events.grouping.graph.building.GraphChangeEvent.MeasurementEndMarker

import java.time.{Duration, Instant}

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector

/** Passes through all GraphChangeEvents, but also outputs a group of events
  * that prune the resultant graph whenever required.
  *
  * There are two potential triggers for pruning. The first one whose
  * requirements are fulfilled will cause a prune to occur, and reset the
  * counter for both triggers. For example, you can configure a prune to occur
  * every 500 measurements or every 12 hours (these are the default settings).
  * If 400 measurements had been seen after 12 hours had passed, a prune would
  * occur on the next measurement, since the time trigger is fulfilled. Once the
  * prune is complete, another will run in another 12 hours, or once another
  * 500 measurements are seen before 12 hours passes.
  *
  * ==Configuration==
  *
  * This class and its children are configured by the `eventGrouping.graph`
  * config key group.
  *
  * - `pruneIntervalCount`: Once this many measurements are seen, a prune is
  * triggered. Default 500.
  *
  * - `pruneIntervalTime`: The number of seconds that must pass before a prune
  * is triggered. Default 43200s (12 hours).
  */
abstract class GraphPruneEventGenerator
  extends ProcessFunction[GraphChangeEvent, GraphChangeEvent]
          with CheckpointedFunction
          with HasFlinkConfig
          with Logging {
  override val flinkName: String = "Graph Pruner"
  override val flinkUid: String = "graph-prune"
  override val configKeyGroup: String = "eventGrouping.graph"

  var lastPruneTime: Instant = Instant.EPOCH
  var measurementsSinceLastPrune: Long = 0

  private var lastPruneTimeState: ListState[Instant] = _
  private var measurementsSinceLastPruneState: ListState[Long] = _

  /** How often we prune, in measurement count */
  @transient private lazy val pruneIntervalCount: Long =
  configWithOverride(getRuntimeContext).getLong(s"$configKeyGroup.pruneIntervalCount")

  /** How often we prune, in event time */
  @transient private lazy val pruneIntervalTime: Duration =
  Duration.ofSeconds(
    configWithOverride(getRuntimeContext).getLong(s"$configKeyGroup.pruneIntervalTime"))

  def onProcessElement(value: GraphChangeEvent): Unit

  def doPrune(
    currentTime  : Instant,
    ctx          : ProcessFunction[GraphChangeEvent, GraphChangeEvent]#Context,
    out          : Collector[GraphChangeEvent]
  ): Unit

  override def processElement(
    value: GraphChangeEvent,
    ctx  : ProcessFunction[GraphChangeEvent, GraphChangeEvent]#Context,
    out  : Collector[GraphChangeEvent]
  ): Unit = {
    onProcessElement(value)
    out.collect(value)

    value match {
      case MeasurementEndMarker(time) =>
        measurementsSinceLastPrune += 1
        if (measurementsSinceLastPrune >= pruneIntervalCount) {
          doPrune(time, ctx, out)
          lastPruneTime = time
          measurementsSinceLastPrune = 0
        }
        else {
          lastPruneTime match {
            // If this is our first measurement, we just set the time so that we
            // prune later after the normal timeout.
            case Instant.EPOCH => lastPruneTime = time
            // If it's been long enough, go ahead and prune the graph.
            case _ if Duration.between(lastPruneTime, time).compareTo(pruneIntervalTime) > 0 =>
              doPrune(time, ctx, out)
              lastPruneTime = time
              measurementsSinceLastPrune = 0
            // Otherwise, do nothing.
            case _ =>
          }
        }
      case _ =>
    }
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    lastPruneTimeState.clear()
    lastPruneTimeState.add(lastPruneTime)
    measurementsSinceLastPruneState.clear()
    measurementsSinceLastPruneState.add(measurementsSinceLastPrune)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    lastPruneTimeState = context.getOperatorStateStore
      .getUnionListState(new ListStateDescriptor("lastPruneTime", classOf[Instant]))

    measurementsSinceLastPruneState = context.getOperatorStateStore
      .getUnionListState(new ListStateDescriptor("measurementsSinceLastPrune", classOf[Long]))

    if (context.isRestored) {
      lastPruneTimeState.get.forEach(entry => lastPruneTime = entry)
      measurementsSinceLastPruneState.get.forEach(entry => measurementsSinceLastPrune = entry)
    }
  }
}
