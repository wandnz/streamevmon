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

import nz.net.wand.streamevmon.connectors.postgres.schema.AsNumber
import nz.net.wand.streamevmon.events.grouping.graph.building.{BuildsGraph, GraphChangeEvent}
import nz.net.wand.streamevmon.events.grouping.graph.impl.GraphType.{EdgeT, GraphT, VertexT}
import nz.net.wand.streamevmon.flink.HasFlinkConfig

import java.awt.Color
import java.io.File

import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.dot.DOTExporter

import scala.collection.JavaConverters._

/** Exports the graph to a .dot file every so often.
  *
  * The configuration value `eventGrouping.graph.exportInterval` is the number
  * of measurements that should be received between each export.
  *
  * `eventGrouping.graph.exportLocation` is the file to save the graph to.
  */
class GraphDotExporter
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
    GraphDotExporter.exportGraph(graph, exportLocation)
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    snapshotGraphState(context)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    initializeGraphState(context)
  }
}

object GraphDotExporter {
  /** Exports a graph to a .dot file, with the vertices coloured according to
    * which AS they belong to.
    */
  def exportGraph(
    graph: GraphT,
    file : File
  ): Unit = {
    val exporter = new DOTExporter[VertexT, EdgeT]

    // We want to make sure that nodes have their UIDs printed as their names.
    exporter.setVertexIdProvider(entry => s""" "${entry.toString}" """.trim)

    // We also want to give them a colour defined by AS. This gives us a lookup
    // of AS numbers to sequential unique IDs, which lets us get reasonably-
    // distinct colours by distributing them evenly around hue-space.
    val asNumberIndex: Map[Int, Int] = graph.vertexSet.asScala.toList
      .flatMap(host => host.allAsNumbers)
      .flatMap(_.number)
      .toSet
      .zipWithIndex
      .toMap

    def getAsColor(asn: AsNumber): String = {
      asn.number match {
        case None => "#FFFFFF"
        case Some(num) =>
          val hue = (0.8 * (asNumberIndex(num).toDouble / asNumberIndex.size.toDouble) + 0.1) % 1
          val color = Color.getHSBColor(hue.toFloat, 0.5f, 0.95f)
          f"#${color.getRed}%02X${color.getGreen}%02X${color.getBlue}%02X"
      }
    }


    exporter.setVertexAttributeProvider { entry =>
      val isProbablyAmplet = entry.hostnames.exists(_.contains("amp"))
      Map(
        "style" -> DefaultAttribute.createAttribute("filled"),
        // Hosts that are probably amplets are squares, while anything else is round.
        "shape" -> DefaultAttribute.createAttribute(if (isProbablyAmplet) {
          "box"
        }
        else {
          "oval"
        }),
        "fillcolor" -> DefaultAttribute.createAttribute {
          if (entry.hostnames.nonEmpty) {
            // Hosts that have known hostnames get a unique, bright colour.
            "#FF0000"
          }
          else if (entry.primaryAsNumber.nonEmpty) {
            getAsColor(entry.primaryAsNumber.get)
          }
          else {
            getAsColor(AsNumber.Missing)
          }
        }
      ).asJava
    }

    exporter.setEdgeIdProvider(_.lastSeen.toString)

    exporter.exportGraph(graph, file)
  }
}
