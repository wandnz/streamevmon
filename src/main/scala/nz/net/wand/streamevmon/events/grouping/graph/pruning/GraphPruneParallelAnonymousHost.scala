package nz.net.wand.streamevmon.events.grouping.graph.pruning

import nz.net.wand.streamevmon.events.grouping.graph.{EdgeWithLastSeen, Host}
import nz.net.wand.streamevmon.Logging

import org.jgrapht.{Graph, GraphPath}
import org.jgrapht.alg.shortestpath.AllDirectedPaths

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.mutable

/** Merges duplicate anonymous hosts by finding anonymous parallel paths with
  * the same length.
  *
  * If a host does not respond to a traceroute query, it is represented as an
  * anonymous host, which uses the location in a traceroute path as its UID. If
  * one of these anonymous hosts is part of multiple AMP traceroute paths, it
  * will appear once in each of these paths.
  *
  * This results in cases where an identified host has many children, each of
  * which only have one child until the next identified host is reached. These
  * parallel paths are very likely to be the same set of hosts, and having them
  * as multiple nodes is useless to us since their paths will never branch further.
  *
  * @param mergedHosts  Get a reference to this from an [[nz.net.wand.streamevmon.events.grouping.graph.AliasResolver AliasResolver]].
  * @param onUpdateHost A function that updates the old host with the new one in the graph.
  */
class GraphPruneParallelAnonymousHost[
  VertexT <: Host,
  EdgeT <: EdgeWithLastSeen,
  GraphT <: Graph[VertexT, EdgeT]
](
  mergedHosts: mutable.Map[String, VertexT],
  onUpdateHost: (VertexT, VertexT) => Unit
) extends GraphPruneApproach[VertexT, EdgeT, GraphT]
          with Logging {

  /** Travels up the graph until a single parent with multiple children is found.
    * If there are multiple parents, then there is no single direct parent and
    * we return None.
    * If there are no parents, then we return None.
    *
    * We keep count of depth (plus a bit) so the search algorithm used later to
    * find paths between top and bottom hosts has a limit.
    */
  @tailrec
  private def findDirectParentWithMultipleChildren(
    graph: GraphT,
    vertex: VertexT,
    depth: Int = 3
  ): Option[(VertexT, Int)] = {
    // If the current node has multiple children, we've found our target.
    if (graph.outDegreeOf(vertex) > 1) {
      Some((vertex, depth))
    }
    else {
      val incoming = graph.incomingEdgesOf(vertex)
      if (incoming.size != 1) {
        None
      }
      else {
        // If there is a single parent, try that one.
        findDirectParentWithMultipleChildren(
          graph,
          graph.getEdgeSource(incoming.asScala.head),
          depth + 1
        )
      }
    }
  }

  override def prune(graph: GraphT): Unit = {
    val allPaths = new AllDirectedPaths(graph)

    // Get all the vertices...
    val vertices: mutable.Set[VertexT] = graph.vertexSet.asScala
    // that aren't anonymous.
    val nonAnonymousVertices: mutable.Set[VertexT] = vertices.filter(_.ampTracerouteUids.isEmpty)
    // Get only the ones that have multiple anonymous direct parents.
    val multipleIncomingEdgesFromAnonymousVertices: mutable.Set[VertexT] = nonAnonymousVertices
      .filter { v =>
        graph
          .incomingEdgesOf(v)
          .asScala
          .count { e =>
            graph
              .getEdgeSource(e)
              .ampTracerouteUids
              .nonEmpty
          } > 1
      }
    // Make a map of the vertices from the previous step to their direct parents.
    val bottomHostToAnonymousDirectParents: Map[VertexT, mutable.Set[VertexT]] = multipleIncomingEdgesFromAnonymousVertices
      .map { v =>
        (
          v,
          graph
            .incomingEdgesOf(v)
            .asScala
            .map(e => graph.getEdgeSource(e))
        )
      }
      .toMap
    // For each bottom host, find the nearest common single ancestor of its direct parents.
    // This should give us the place where the graph originally split into the parallel
    // anonymous children.
    val bottomHostToCommonAncestors: Map[VertexT, mutable.Set[(VertexT, Int)]] = bottomHostToAnonymousDirectParents
      .map { case (bottomHost, directParents) =>
        // We lose the direct parents in this step, but that's OK since we're
        // getting them back as part of the paths between the common ancestor
        // the bottom host in the next step.
        (
          bottomHost,
          directParents.flatMap(findDirectParentWithMultipleChildren(graph, _))
        )
      }
    // For each bottom host, we have a mutable.Set[VertexT] of the common ancestors
    // of its anonymous direct parents. We find all the paths between each of
    // these ancestors and the bottom host.
    val pathsFromCommonAncestorsToBottomHosts: Map[VertexT, Map[VertexT, mutable.Buffer[GraphPath[VertexT, EdgeT]]]] = bottomHostToCommonAncestors
      .map { case (bottomHost, commonAncestors) =>
        (
          bottomHost,
          commonAncestors.map { ancestor =>
            (
              ancestor._1,
              allPaths.getAllPaths(ancestor._1, bottomHost, true, ancestor._2).asScala
            )
          }.toMap
        )
      }
    // Filter out any paths which don't strictly contain anonymous hosts. We're
    // only trying to merge parallel anonymous paths.
    val relevantPathsFromCommonAncestorsToBottomHosts: Map[VertexT, Map[VertexT, Iterable[GraphPath[VertexT, EdgeT]]]] = pathsFromCommonAncestorsToBottomHosts
      .map { case (bottomHost, mapCommonAncestorsToPaths) =>
        (
          bottomHost,
          mapCommonAncestorsToPaths.map { case (ancestor, paths) =>
            (
              ancestor,
              paths.filter { path =>
                path
                  .getVertexList
                  .asScala
                  // We don't care whether the top and bottom hosts are anonymous,
                  // since they're not gonna be touched regardless.
                  .drop(1)
                  .dropRight(1)
                  .forall(_.ampTracerouteUids.nonEmpty)
              }
            )
          }
        )
      }
    // Group the paths by their length, and remove any that have unique lengths.
    // We can't merge them with anything if they're unique.
    val pathsGroupedByLength: Map[VertexT, Map[VertexT, Iterable[Iterable[GraphPath[VertexT, EdgeT]]]]] = relevantPathsFromCommonAncestorsToBottomHosts
      .map { case (bottomHost, mapTopHostToPaths) =>
        (
          bottomHost,
          mapTopHostToPaths.map { case (topHost, paths) =>
            (
              topHost,
              paths
                .groupBy(_.getLength)
                .filter(_._2.size > 1)
                .values
            )
          }
        )
      }

    // Drop all the information about the top and bottom hosts. We don't need
    // them anymore. This leaves us with a collection of grouped GraphPaths
    // that should be merged.
    val pathsToMerge: Iterable[Iterable[GraphPath[VertexT, EdgeT]]] = pathsGroupedByLength
      .flatMap { case (_, mapTopHostToGroupedPaths) =>
        mapTopHostToGroupedPaths.flatMap { case (_, groupedPaths) =>
          groupedPaths
        }
      }

    // Turn the GraphPaths into lists of the vertices they go through, and then
    // transpose the array to pair up the matching hosts.
    val hostsToMerge: Iterable[Iterable[Iterable[VertexT]]] = pathsToMerge
      .map { group =>
        group.map { path =>
          path.getVertexList.asScala.drop(1).dropRight(1)
        }.transpose
      }

    // Perform the merge. We need to fold them into Host objects instead of
    // VertexT because mergeWith returns a Host.
    hostsToMerge.map(_.map { items =>
      val mergedItems = items
        .drop(1)
        .foldLeft(
          items.head.asInstanceOf[Host]
        )(
          (a, b) => a.mergeAnonymous(b)
        ).asInstanceOf[VertexT]
      items.foreach { item =>
        mergedHosts.remove(item.uid)
        mergedHosts.put(item.uid, mergedItems)
        onUpdateHost(item, mergedItems)
      }
    })
  }
}
