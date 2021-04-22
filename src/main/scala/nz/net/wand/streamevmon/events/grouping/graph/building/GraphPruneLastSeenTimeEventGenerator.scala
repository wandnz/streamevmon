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

import nz.net.wand.streamevmon.events.grouping.graph.building.GraphChangeEvent.{RemoveOldEdges, RemoveUnconnectedVertices}

import java.time.{Duration, Instant}

import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector

/** Passes through all GraphChangeEvents, but also outputs a group of events
  * that prune the resultant graph whenever required.
  *
  * This class prunes edges which haven't been seen for a while. Use the
  * `eventGrouping.graph.pruneAge` configuration key to change the maximum age
  * of edges in seconds. The default is 1200.
  */
class GraphPruneLastSeenTimeEventGenerator
  extends GraphPruneEventGenerator {

  /** How old an edge must be before it gets pruned */
  @transient lazy val pruneAge: Duration =
  Duration.ofSeconds(configWithOverride(getRuntimeContext).getLong(s"$configKeyGroup.pruneAge"))

  override def onProcessElement(value: GraphChangeEvent): Unit = {}

  override def doPrune(
    currentTime           : Instant,
    ctx                   : ProcessFunction[GraphChangeEvent, GraphChangeEvent]#Context,
    out                   : Collector[GraphChangeEvent]
  ): Unit = {
    out.collect(RemoveOldEdges(currentTime.minus(pruneAge)))
    out.collect(RemoveUnconnectedVertices())
  }
}
