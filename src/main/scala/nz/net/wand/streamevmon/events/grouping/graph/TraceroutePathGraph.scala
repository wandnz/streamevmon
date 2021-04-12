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

package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.connectors.postgres.schema.AsInetPath
import nz.net.wand.streamevmon.events.grouping.graph.GraphType._
import nz.net.wand.streamevmon.flink.HasFlinkConfig

import java.time.{Duration, Instant}

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.util.Collector

import scala.collection.JavaConverters._
import scala.collection.mutable

/** Attempts to place events on a topological network graph.
  *
  * The first input is the stream of new events which should be grouped.
  *
  * The second input is a stream of new AsInetPaths which are generated from
  * Traceroute measurements. Duplicates are acceptable,
  *
  * Currently, the "placing events" functionality is unimplemented, and this
  * just acts as a passthrough function for Events that takes up a lot of memory.
  *
  * ==Configuration==
  *
  * This ProcessFunction is configured by the `eventGrouping.graph` config key
  * group.
  *
  * If any of the prune interval conditions are fulfilled, a prune will occur.
  * When a prune occurs, all the condition counters are reset.
  *
  * - `pruneIntervalCount`: Once this many new paths are received, the graph
  * will be pruned.
  * - `pruneIntervalTime`: This many seconds must pass between graph prunings.
  * This is based on the event time of events and paths that are received.
  * - `pruneAge`: An edge must be this many seconds old before it gets pruned.
  *
  * @see [[nz.net.wand.streamevmon.events.grouping.graph.GraphConstructionLogic GraphConstructionLogic]]
  *      for a description of the pruning methods used.
  */
class TraceroutePathGraph[EventT <: Event]
  extends CoProcessFunction[EventT, AsInetPath, Event]
          with GraphConstructionLogic
          with CheckpointedFunction
          with HasFlinkConfig
          with Logging {
  override val flinkName: String = "Traceroute-Path Graph"
  override val flinkUid: String = "traceroute-path-graph"
  override val configKeyGroup: String = "eventGrouping.graph"

  var graph: GraphT = _

  var lastPruneTime: Instant = Instant.EPOCH
  var measurementsSinceLastPrune: Long = 0

  @transient private lazy val aliasResolver: AliasResolver = AliasResolver(configWithOverride(getRuntimeContext))

  override def getMergedHosts: mutable.Map[String, VertexT] = aliasResolver.mergedHosts

  /** How often we prune, in measurement count */
  @transient private lazy val pruneIntervalCount: Long =
  configWithOverride(getRuntimeContext).getLong(s"$configKeyGroup.pruneIntervalCount")

  /** How often we prune, in event time */
  @transient private lazy val pruneIntervalTime: Duration =
  Duration.ofSeconds(configWithOverride(getRuntimeContext).getLong(s"$configKeyGroup.pruneIntervalTime"))

  /** How old an edge must be before it gets pruned */
  @transient private lazy val pruneAge: Duration =
  Duration.ofSeconds(configWithOverride(getRuntimeContext).getLong(s"$configKeyGroup.pruneAge"))

  override def open(parameters: Configuration): Unit = {
    // We can't go sharing inputs with other parallel instances, since that
    // would mean everyone has an incomplete graph.
    if (getRuntimeContext.getNumberOfParallelSubtasks > 1) {
      throw new IllegalStateException("Parallelism for this CoProcessFunction must be 1.")
    }
  }

  /** Executes supported pruning methods if one of the supported conditions is
    * fulfilled
    *
    * @see `pruneIntervalCount`, `pruneIntervalTime`
    * @see [[nz.net.wand.streamevmon.events.grouping.graph.GraphConstructionLogic GraphConstructionLogic]]
    */
  def pruneIfRequired(currentTime: Instant): Unit = {
    measurementsSinceLastPrune += 1

    if (measurementsSinceLastPrune >= pruneIntervalCount) {
      pruneGraphByLastSeenTime(graph, pruneAge, currentTime)
      pruneGraphByParallelAnonymousHostPathMerge(graph)
      lastPruneTime = currentTime
      measurementsSinceLastPrune = 0
    }
    else {
      lastPruneTime match {
        // If this is our first measurement, we just set the time so that we
        // prune later after the normal timeout.
        case Instant.EPOCH => lastPruneTime = currentTime
        // If it's been long enough, go ahead and prune the graph.
        case _ if Duration.between(lastPruneTime, currentTime).compareTo(pruneIntervalTime) > 0 =>
          pruneGraphByLastSeenTime(graph, pruneAge, currentTime)
          pruneGraphByParallelAnonymousHostPathMerge(graph)
          lastPruneTime = currentTime
          measurementsSinceLastPrune = 0
        // Otherwise, do nothing.
        case _ =>
      }
    }
  }

  override def processElement1(
    value: EventT,
    ctx  : CoProcessFunction[EventT, AsInetPath, Event]#Context,
    out  : Collector[Event]
  ): Unit = {
    pruneIfRequired(value.time)
    out.collect(value)
  }

  /** Adds an AsInetPath to the graph. New hosts will become new vertices, and
    * missing edges will be added. Gives no output. Prunes if required, so this
    * function sometimes takes longer than usual.
    */
  override def processElement2(
    value: AsInetPath,
    ctx  : CoProcessFunction[EventT, AsInetPath, Event]#Context,
    out  : Collector[Event]
  ): Unit = {
    addAsInetPathToGraph(graph, aliasResolver, value)
    pruneIfRequired(value.measurement.time)
  }

  // == CheckpointedFunction implementation ==

  private var graphState: ListState[GraphT] = _
  private var mergedHostsState: ListState[VertexT] = _
  private var lastPruneTimeState: ListState[Instant] = _
  private var measurementsSinceLastPruneState: ListState[Long] = _

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    graphState.clear()
    graphState.add(graph)
    mergedHostsState.clear()
    mergedHostsState.addAll(getMergedHosts.values.toSeq.asJava)
    lastPruneTimeState.clear()
    lastPruneTimeState.add(lastPruneTime)
    measurementsSinceLastPruneState.clear()
    measurementsSinceLastPruneState.add(measurementsSinceLastPrune)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    graphState = context
      .getOperatorStateStore
      .getUnionListState(new ListStateDescriptor("graph", classOf[GraphT]))

    mergedHostsState = context
      .getOperatorStateStore
      .getUnionListState(new ListStateDescriptor("mergedHosts", classOf[VertexT]))

    lastPruneTimeState = context
      .getOperatorStateStore
      .getUnionListState(new ListStateDescriptor("lastPruneTime", classOf[Instant]))

    measurementsSinceLastPruneState = context
      .getOperatorStateStore
      .getUnionListState(new ListStateDescriptor("measurementsSinceLastPrune", classOf[Long]))

    if (context.isRestored) {
      graphState.get.forEach(entry => graph = entry)
      mergedHostsState.get.forEach(entry => getMergedHosts.put(entry.uid, entry))
      lastPruneTimeState.get.forEach(entry => lastPruneTime = entry)
      measurementsSinceLastPruneState.get.forEach(entry => measurementsSinceLastPrune = entry)
    }
    else {
      graph = new GraphT(classOf[EdgeT])
      graph.setEdgeSupplier(new NoReflectionUnusableEdgeSupplier[EdgeT])
    }
  }
}
