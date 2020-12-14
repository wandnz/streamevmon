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

import java.awt.Color
import java.io.File

import org.jgrapht.graph.DefaultDirectedWeightedGraph
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.dot.DOTExporter

import scala.collection.JavaConverters._

/** Exports a graph to a .dot file, with the vertexes coloured according to what
  * AS they belong to.
  */
object AmpletGraphDotExporter {
  def exportGraph[VertexT <: Host, EdgeT <: EdgeWithLastSeen](
    graph: DefaultDirectedWeightedGraph[VertexT, EdgeT],
    file : File
  ): Unit = {
    val exporter = new DOTExporter[VertexT, EdgeT]

    // We want to make sure that nodes have their addresses printed as their
    // names.
    exporter.setVertexIdProvider(entry => s""" "${entry.toString}" """.trim)

    // We also want to give them a colour defined by AS.
    val asNumberIndex: Map[Int, Int] = graph.vertexSet.asScala.toList
      .flatMap(host => host.addresses.map(_._2))
      .flatMap(_.number)
      .toSet
      .zipWithIndex
      .toMap

    // These colours are evenly distributed around hue-space, so they're all
    // reasonably different and black labels should be legible against them.
    def getAsColor(asn: AsNumber): String = {
      asn.number match {
        case None => "#FFFFFF"
        case Some(num) =>
          val hue = (0.8 * (asNumberIndex(num).toDouble / asNumberIndex.size.toDouble) + 0.1) % 1
          val color = Color.getHSBColor(hue.toFloat, 0.5f, 0.95f)
          f"#${color.getRed}%02X${color.getGreen}%02X${color.getBlue}%02X"
      }
    }

    val namedColor = "#FF0000"

    exporter.setVertexAttributeProvider { entry =>
      val isProbablyAmplet = entry.hostnames.exists(_.contains("amp"))
      Map(
        "style" -> DefaultAttribute.createAttribute("filled"),
        "shape" -> DefaultAttribute.createAttribute(if (isProbablyAmplet) {
          "box"
        }
        else {
          "oval"
        }),
        "fillcolor" -> DefaultAttribute.createAttribute {
          if (entry.hostnames.nonEmpty) {
            namedColor
          }
          else if (entry.addresses.nonEmpty) {
            getAsColor(entry.addresses.head._2)
          }
          else {
            getAsColor(AsNumber.Missing)
          }
        }
      ).asJava
    }

    exporter.setEdgeIdProvider(_.lastSeen.toString)

    // A dot file is just a listing of vertices and edges, and contains nothing
    // about layout. That's left to the renderer, which isn't our problem.
    exporter.exportGraph(graph, file)
  }
}
