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
import nz.net.wand.streamevmon.events.grouping.graph.building.GraphChangeEvent._
import nz.net.wand.streamevmon.events.grouping.graph.pruning.AliasResolver

import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector

/** Performs alias resolution on all GraphChangeEvents passed to it.
  *
  * If a MergeVertices is provided, it will add an additional alias.
  */
class GraphChangeAliasResolution
  extends ProcessFunction[GraphChangeEvent, GraphChangeEvent]
          with CheckpointedFunction
          with HasFlinkConfig
          with Logging {
  override val flinkName: String = "Graph Change Alias Resolution"
  override val flinkUid: String = "graph-change-alias-resolution"
  override val configKeyGroup: String = "eventGrouping.graph"

  @transient lazy val aliasResolver: AliasResolver = AliasResolver(configWithOverride(getRuntimeContext))

  override def processElement(
    value: GraphChangeEvent,
    ctx: ProcessFunction[GraphChangeEvent, GraphChangeEvent]#Context,
    out: Collector[GraphChangeEvent]
  ): Unit = {
    value match {
      case e: MergeVertices =>
        val resolved = e.vertices.map(aliasResolver.resolve(_))
        if (resolved.size > 1) {
          aliasResolver.addKnownAliases(e.merged, resolved)
          out.collect(MergeVertices(resolved))
        }
      // else {
      // out.collect(DoNothing())
      // }
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
      case RemoveOldEdges(_) => out.collect(value)
      case MeasurementEndMarker(_) => out.collect(value)
      case _: NoArgumentGraphChangeEvent => out.collect(value)
    }
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    aliasResolver.snapshotState(context)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    aliasResolver.initializeState(context)
  }
}
