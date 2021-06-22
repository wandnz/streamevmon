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

package nz.net.wand.streamevmon.events.grouping.graph.pruning

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.events.grouping.graph.impl.EdgeWithLastSeen

import java.time.{Duration, Instant}

import org.jgrapht.Graph
import org.jgrapht.alg.connectivity.ConnectivityInspector

import scala.collection.JavaConverters._

/** Pruning algorithm which removes edges when they haven't been seen in a long
  * time, then removes the now disconnected vertices.
  *
  * This behaviour is also implemented with the two GraphChangeEvents
  * [[nz.net.wand.streamevmon.events.grouping.graph.building.GraphChangeEvent.RemoveOldEdges RemoveOldEdges]]
  * and
  * [[nz.net.wand.streamevmon.events.grouping.graph.building.GraphChangeEvent.RemoveUnconnectedVertices RemoveUnconnectedVertices]]
  *
  * @param pruneAge    How old an edge must be before it is pruned
  * @param currentTime The current event time.
  */
class GraphPruneLastSeenTime[
  VertexT,
  EdgeT <: EdgeWithLastSeen,
  GraphT <: Graph[VertexT, EdgeT]
](
  pruneAge   : Duration,
  currentTime: Instant
) extends Logging {
  def prune(graph: GraphT): Unit = {
    val startTime = System.nanoTime()

    graph.edgeSet.asScala
      // Convert the edge list to a solid list instead of a lazy collection so
      // that we're not modifying the graph during an iteration over its edges.
      .toList
      .foreach { edge =>
        // If it's old enough, get rid of it.
        if (Duration.between(edge.lastSeen, currentTime).compareTo(pruneAge) > 0) {
          graph.removeEdge(edge)
        }
      }

    // The connectivity inspector can tell us about all sets of connected
    // elements within the graph. We preserve the one with the most elements,
    // since it's the main part of the graph that shouldn't be pruned.
    graph.removeAllVertices(
      new ConnectivityInspector(graph).connectedSets.asScala
        .sortBy(_.size)
        .dropRight(1)
        .flatMap(_.asScala)
        .toSet
        .asJavaCollection
    )

    val endTime = System.nanoTime()
    logger.trace(s"Pruning graph took ${Duration.ofNanos(endTime - startTime).toMillis}ms")
  }
}
