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

import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector

class BasicBuildsGraphImpl
  extends ProcessFunction[GraphChangeEvent, GraphChangeEvent]
          with BuildsGraph
          with CheckpointedFunction {

  override def processElement(
    value: GraphChangeEvent,
    ctx  : ProcessFunction[GraphChangeEvent, GraphChangeEvent]#Context,
    out  : Collector[GraphChangeEvent]
  ): Unit = {
    receiveGraphChangeEvent(value)
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    snapshotGraphState(context)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    initializeGraphState(context)
  }
}
