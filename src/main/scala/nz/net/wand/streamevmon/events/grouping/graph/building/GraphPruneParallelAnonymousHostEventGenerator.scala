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

import nz.net.wand.streamevmon.events.grouping.graph.building.GraphChangeEvent.{DoPruneParallelAnonymousHosts, MergeVertices}

import java.time.Instant

import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

/** Passes through all GraphChangeEvents, but also outputs a group of events
  * that prune the resultant graph whenever required.
  *
  * This class prunes parallel chains of anonymous hosts. For more details, see
  * [[nz.net.wand.streamevmon.events.grouping.graph.pruning.GraphPruneParallelAnonymousHost GraphPruneParallelAnonymousHost]].
  *
  * This class additionally sends
  * [[nz.net.wand.streamevmon.events.grouping.graph.building.GraphChangeEvent.MergeVertices MergeVertices]]
  * events to a side output,
  * using the tag declared as `mergeVerticesEventOutputTag`. If this class is
  * used downstream from a
  * [[GraphChangeAliasResolution]] module, this side output should be connected
  * to the AliasResolution module's second input. This will create a loop, which
  * will be handled correctly.
  */
class GraphPruneParallelAnonymousHostEventGenerator
  extends GraphPruneEventGenerator
          with BuildsGraph {
  val mergeVerticesEventOutputTag = new OutputTag[MergeVertices]("merge-vertices-events")

  override def onProcessElement(value: GraphChangeEvent): Unit = {
    receiveGraphChangeEvent(value)
  }

  override def doPrune(
    currentTime: Instant,
    ctx        : ProcessFunction[GraphChangeEvent, GraphChangeEvent]#Context,
    out        : Collector[GraphChangeEvent]
  ): Unit = {
    val events = DoPruneParallelAnonymousHosts().getMergeVertexEvents(graph)
    DoPruneParallelAnonymousHosts().applyMergeVertexEvents(graph, events)
    events.foreach { e =>
      out.collect(e)
      ctx.output(mergeVerticesEventOutputTag, e)
    }
  }
}
