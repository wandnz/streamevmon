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

import nz.net.wand.streamevmon.events.grouping.graph.impl.GraphType.{EdgeT, GraphT, VertexT}
import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.traits.MeasurementMeta

import org.jgrapht.alg.shortestpath.{ALTAdmissibleHeuristic, AStarShortestPath}

import scala.collection.JavaConverters._

/** Calculates the "distance" between two measurement streams. This is a
  * reasonably arbitrary heuristic, and should not be relied on to represent
  * any useful value.
  */
object DistanceBetweenStreams {
  private case class RelevantLocations(
    source     : VertexT,
    destination: VertexT
  )

  private def getVertexByName(graph: GraphT, name: String): Option[VertexT] = {
    graph
      .vertexSet
      .asScala
      .find { v =>
        v.hostnames.contains(name) ||
          v.addresses.exists(_._1.address.mkString(".") == name)
      }
  }

  private def getRelevantLocations(graph: GraphT, item: MeasurementMeta): RelevantLocations = {
    val (source, destination) = item match {
      case i: DNSMeta => (i.source, i.destination)
      case i: HTTPMeta => (i.source, i.destination)
      case i: ICMPMeta => (i.source, i.destination)
      case i: TCPPingMeta => (i.source, i.destination)
      case i: TracerouteMeta => (i.source, i.destination)
    }
    RelevantLocations(getVertexByName(graph, source).get, getVertexByName(graph, destination).get)
  }

  def get(graph: GraphT, a: MeasurementMeta, b: MeasurementMeta): Double = {
    val aLocations = getRelevantLocations(graph, a)
    val bLocations = getRelevantLocations(graph, b)
    val pathFinder = new AStarShortestPath(
      graph,
      new ALTAdmissibleHeuristic[VertexT, EdgeT](
        graph,
        Set[VertexT](
          aLocations.source,
          aLocations.destination,
          bLocations.source,
          bLocations.destination
        ).asJava
      )
    )
    val sourcesDistance = pathFinder.getPath(aLocations.source, bLocations.source).getLength
    val destsDistance = pathFinder.getPath(aLocations.source, bLocations.source).getLength
    sourcesDistance + destsDistance
  }
}
