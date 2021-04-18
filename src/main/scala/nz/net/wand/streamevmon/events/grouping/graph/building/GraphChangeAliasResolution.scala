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
import nz.net.wand.streamevmon.events.grouping.graph.AliasResolver
import nz.net.wand.streamevmon.events.grouping.graph.GraphType.VertexT
import nz.net.wand.streamevmon.events.grouping.graph.building.GraphChangeEvent._

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.util.Collector

import scala.collection.JavaConverters._

/** Performs alias resolution on all GraphChangeEvents passed to it. You can
  * also supply it with additional aliases that were found using other methods,
  * and it will keep track of them. Note that anything supplied to the second
  * input will not be passed on, as it is assumed to have been already applied
  * to the graph. Supply it to the first input to force passthrough.
  */
class GraphChangeAliasResolution
  extends CoProcessFunction[GraphChangeEvent, MergeVertices, GraphChangeEvent]
          with CheckpointedFunction
          with HasFlinkConfig
          with Logging {
  override val flinkName: String = "Graph Change Alias Resolution"
  override val flinkUid: String = "graph-change-alias-resolution"
  override val configKeyGroup: String = "eventGrouping.graph"

  @transient private lazy val aliasResolver: AliasResolver = AliasResolver(configWithOverride(getRuntimeContext))

  private var mergedHostsState: ListState[VertexT] = _

  override def processElement1(
    value: GraphChangeEvent,
    ctx  : CoProcessFunction[GraphChangeEvent, MergeVertices, GraphChangeEvent]#Context,
    out  : Collector[GraphChangeEvent]
  ): Unit = {
    value match {
      case e: MergeVertices =>
        processElement2(e, ctx, out)
        out.collect(e)
      case AddVertex(vertex) => out.collect(AddVertex(aliasResolver.resolve(vertex)))
      case RemoveVertex(vertex) => out.collect(RemoveVertex(aliasResolver.resolve(vertex)))
      case event: UpdateVertex => out.collect(
        UpdateVertex.create(aliasResolver.resolve(event.before), aliasResolver.resolve(event.after))
      )
      case AddOrUpdateEdge(start, end, edge) => out.collect(
        AddOrUpdateEdge(aliasResolver.resolve(start), aliasResolver.resolve(end), edge)
      )
      case RemoveEdgeByVertices(start, end) => out.collect(
        RemoveEdgeByVertices(aliasResolver.resolve(start), aliasResolver.resolve(end))
      )
      case RemoveEdge(_) => out.collect(value)
      case MeasurementEndMarker(_) => out.collect(value)
      case _: NoArgumentGraphChangeEvent => out.collect(value)
    }
  }

  override def processElement2(
    value                           : MergeVertices,
    ctx                             : CoProcessFunction[GraphChangeEvent, MergeVertices, GraphChangeEvent]#Context,
    out                             : Collector[GraphChangeEvent]
  ): Unit = {
    aliasResolver.addKnownAliases(value.merged, value.vertices)
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    mergedHostsState.clear()
    mergedHostsState.addAll(aliasResolver.mergedHosts.values.toSeq.asJava)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    mergedHostsState = context
      .getOperatorStateStore
      .getUnionListState(new ListStateDescriptor("mergedHosts", classOf[VertexT]))

    if (context.isRestored) {
      mergedHostsState.get.forEach(entry => aliasResolver.mergedHosts.put(entry.uid, entry))
    }
  }
}
