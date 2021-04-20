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

import nz.net.wand.streamevmon.events.grouping.graph.GraphType._
import nz.net.wand.streamevmon.events.grouping.graph.NoReflectionUnusableEdgeSupplier

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}

/** Inherit this trait to properly receive [[GraphChangeEvent]]s. Whenever one
  * is received, it should be passed to `receiveGraphChangeEvent`. The graph
  * can be accessed using the `graph` field, which should not be altered
  * manually in order to maintain consistency.
  *
  * If the inheriting class is a
  * [[org.apache.flink.streaming.api.checkpoint.CheckpointedFunction CheckpointedFunction]],
  * the `snapshotGraphState` and `initializeGraphState` functions should be
  * called from the corresponding override functions.
  */
trait BuildsGraph {
  var graph: GraphT = _

  private var graphState: ListState[GraphT] = _

  def createNewGraph(): Unit = {
    graph = new GraphT(classOf[EdgeT])
    graph.setEdgeSupplier(new NoReflectionUnusableEdgeSupplier[EdgeT])
  }

  def receiveGraphChangeEvent(event: GraphChangeEvent): Unit = {
    if (graph == null) {
      createNewGraph()
    }
    event.apply(graph)
  }

  def snapshotGraphState(context    : FunctionSnapshotContext): Unit = {
    graphState.clear()
    graphState.add(graph)
  }

  def initializeGraphState(context    : FunctionInitializationContext): Unit = {
    graphState = context
      .getOperatorStateStore
      .getUnionListState(new ListStateDescriptor("graph", classOf[GraphT]))

    if (context.isRestored) {
      graphState.get.forEach(entry => graph = entry)
    }
    else {
      createNewGraph()
    }
  }
}
