package nz.net.wand.streamevmon.events.grouping.graph.pruning

import org.jgrapht.Graph

/** Parent trait for graph pruning algorithms. */
trait GraphPruneApproach[VertexT, EdgeT, GraphT <: Graph[VertexT, EdgeT]] {
  def prune(graph: GraphT): Unit
}
