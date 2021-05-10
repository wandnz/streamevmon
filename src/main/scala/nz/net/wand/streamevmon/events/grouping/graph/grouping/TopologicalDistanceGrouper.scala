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

package nz.net.wand.streamevmon.events.grouping.graph.grouping

import nz.net.wand.streamevmon.events.grouping.EventGroup
import nz.net.wand.streamevmon.events.grouping.graph.building.{BuildsGraph, GraphChangeEvent}
import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.traits.MeasurementMeta

import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.util.Collector

class TopologicalDistanceGrouper
  extends CoProcessFunction[(EventGroup, MeasurementMeta), GraphChangeEvent, EventGroup]
          with BuildsGraph
          with CheckpointedFunction
          with HasFlinkConfig {

  override val flinkName: String = "Event Grouping (Topological Distance)"
  override val flinkUid: String = "event-grouping-topological-distance"
  override val configKeyGroup: String = "eventGrouping.topological"

  override def processElement1(
    value: (EventGroup, MeasurementMeta),
    ctx  : CoProcessFunction[(EventGroup, MeasurementMeta), GraphChangeEvent, EventGroup]#Context,
    out  : Collector[EventGroup]
  ): Unit = {
    // First, we should approximate a location on the graph that a stream
    // belongs to.
    // If we have a cached copy of that determination, we should use it.
    // Otherwise, find a new one.

    // Now that we know where the event occurred, we should find events that
    // occurred nearby.
    // We want to exclude events from the stream that this new event belongs to,
    // because they're already grouped together.
    // We should probably also have a depth limit for the nearbyness search.
    // Again, if we have a cached result for the nearby stream lookup, we should
    // use it.
    // Perhaps we want to just store a map of the distance from any stream to
    // any other stream... seems inefficient, but might be our best shot.

    // We should now compare the collection of nearby streams against some
    // configuration values.
    // There should be a maximum limit of intra-group distance, which will
    // very likely be related to the depth limit for the earlier search.
    // We need to also consider temporal factors - do we only group events that
    // overlap in time, or can we include some leeway? Perhaps a fraction of
    // an event group's duration is the way to go here.
  }

  override def processElement2(
    value: GraphChangeEvent,
    ctx  : CoProcessFunction[(EventGroup, MeasurementMeta), GraphChangeEvent, EventGroup]#Context,
    out  : Collector[EventGroup]
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
