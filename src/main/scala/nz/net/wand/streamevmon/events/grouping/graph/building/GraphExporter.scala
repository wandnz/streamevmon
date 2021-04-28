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

import nz.net.wand.streamevmon.events.grouping.graph.AmpletGraphDotExporter
import nz.net.wand.streamevmon.flink.HasFlinkConfig

import java.io.File

import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector

/** Exports the graph to a .dot file every so often.
  *
  * The configuration value `eventGrouping.graph.exportInterval` is the number
  * of measurements that should be received between each export.
  *
  * `eventGrouping.graph.exportLocation` is the file to save the graph to.
  */
class GraphExporter
  extends ProcessFunction[GraphChangeEvent, GraphChangeEvent]
          with BuildsGraph
          with CheckpointedFunction
          with HasFlinkConfig {

  override val flinkName: String = "Graph .dot exporter"
  override val flinkUid: String = "graph-exporter"
  override val configKeyGroup: String = "eventGrouping.graph"

  /** The number of measurements between each export. */
  @transient lazy val exportInterval: Int = configWithOverride(getRuntimeContext).getInt(s"$configKeyGroup.exportInterval")

  @transient lazy val exportLocation: File = new File(configWithOverride(getRuntimeContext).get(s"$configKeyGroup.exportLocation"))

  @transient var measurementsSinceLastExport = 0

  override def processElement(
    value                          : GraphChangeEvent,
    ctx                            : ProcessFunction[GraphChangeEvent, GraphChangeEvent]#Context,
    out                            : Collector[GraphChangeEvent]
  ): Unit = {
    receiveGraphChangeEvent(value)
    out.collect(value)

    value match {
      case GraphChangeEvent.MeasurementEndMarker(_) =>
        measurementsSinceLastExport += 1
        if (measurementsSinceLastExport == exportInterval) {
          measurementsSinceLastExport = 0
          doExport()
        }
      case _ =>
    }
  }

  def doExport(): Unit = {
    AmpletGraphDotExporter.exportGraph(graph, exportLocation)
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    snapshotGraphState(context)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    initializeGraphState(context)
  }
}
