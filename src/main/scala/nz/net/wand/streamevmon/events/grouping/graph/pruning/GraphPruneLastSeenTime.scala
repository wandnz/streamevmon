package nz.net.wand.streamevmon.events.grouping.graph.pruning

import nz.net.wand.streamevmon.events.grouping.graph.EdgeWithLastSeen
import nz.net.wand.streamevmon.Logging

import java.time.{Duration, Instant}

import org.jgrapht.Graph
import org.jgrapht.alg.connectivity.ConnectivityInspector

import scala.collection.JavaConverters._

/** Pruning algorithm which removes edges when they haven't been seen in a long
  * time, then removes the now disconnected vertices.
  *
  * @param pruneAge    How old an edge must be before it is pruned
  * @param currentTime The current event time.
  */
class GraphPruneLastSeenTime[
  VertexT,
  EdgeT <: EdgeWithLastSeen,
  GraphT <: Graph[VertexT, EdgeT]
](
  pruneAge: Duration,
  currentTime: Instant
) extends GraphPruneApproach[VertexT, EdgeT, GraphT]
          with Logging {
  override def prune(graph: GraphT): Unit = {
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
