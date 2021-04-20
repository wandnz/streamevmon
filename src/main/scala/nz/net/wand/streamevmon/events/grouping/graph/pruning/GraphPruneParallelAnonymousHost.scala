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

import nz.net.wand.streamevmon.events.grouping.graph.{EdgeWithLastSeen, Host}
import nz.net.wand.streamevmon.events.grouping.graph.GraphType._
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
    * We keep count of depth so the search algorithm used later to
    * find paths between top and bottom hosts has a limit.
    */
  @tailrec
  private def findDirectParentWithMultipleChildren(
    graph: GraphT,
    vertex: VertexT,
    depth: Int = 0
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
    // We also set a depth limit, which is the distance from the bottom host
    // to the ancestor, plus a bit for good luck.
    val pathsFromCommonAncestorsToBottomHosts: Map[VertexT, Map[VertexT, mutable.Buffer[GraphPath[VertexT, EdgeT]]]] = bottomHostToCommonAncestors
      .map { case (bottomHost, commonAncestors) =>
        (
          bottomHost,
          commonAncestors.map { ancestor =>
            (
              ancestor._1,
              allPaths.getAllPaths(ancestor._1, bottomHost, true, ancestor._2 + 3).asScala
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

/**
  * {{{
  *       x
  *      / \
  *      a b
  *      | |
  *      c d
  *      \ /
  *       y
  * }}}
  *
  * In the above example, vertices `c` and `d` are considered cousins. When
  * `findPotentialCommonAncestor` is called on both `c` and `d`, it will return
  * `x` both times with the same depth. We now know that they share a common
  * ancestor, and we can merge `c` and `d` into a single vertex, as well as
  * merging `a` and `b`.
  *
  * {{{
  *       x
  *      / \
  *     /   \
  *     a    b
  *    / \   |
  *    c d   e
  *    \ \   |
  *     \ \  f
  *      \ | /
  *       \|/
  *        y
  * }}}
  *
  * In this example, `y` is the direct child of `c`, `d`, and `e`. `c` and `d`
  * have the common ancestor `a`, but the ancestor that those three vertices
  * share with the chain of `b`, `e`, and `f` is `x`, which has a different
  * distance to `y` depending on the path taken. Since
  * `findPotentialCommonAncestor` will be called on `c`, `d`, and `f`, it will
  * be found that `c` and `d` can be merged, but `b`, `e`, and `f` cannot. When
  * the function is called on `f`, it returns `x`, but it is the only one to do
  * so.
  *
  * {{{
  *      x
  *     / \
  *    /   \
  *    a   b
  *   / \  |
  *   c d  e
  *   | \  /
  *   f   g
  * }}}
  *
  * In this example, no merges can be made. While `d` and `e` have a common
  * ancestor that has the same distance to `g` with either path, `a` is found
  * along the path between `x` and `g` via `d`. `a` has another child (`c`),
  * but we have no evidence that there is a path from `b` to `c`. `a` must not
  * be merged with `b`, despite them taking the same position in the paths
  * from `a` to `g` via `d` and `e` respectively. As such, calling this
  * function on `d` will return None, indicating that it should not be merged.
  */
object GraphPruneParallelAnonymousHost {

  private def isAnonymous(vertex: VertexT): Boolean = vertex.ampTracerouteUids.nonEmpty

  /** Gets all the non-anonymous vertices from the graph.
    */
  private def getNonAnonymousVertices(graph: GraphT): Set[VertexT] = graph
    .vertexSet
    .asScala
    .filter(!isAnonymous(_))
    .toSet

  /** Removes those non-anonymous vertices which do not have multiple anonymous
    * direct parents. Returns a map of the non-anonymous vertex to a set of
    * its anonymous parents. The size of the set will be greater than 1.
    */
  private def filterVerticesWithMultipleAnonymousParents(
    graph   : GraphT,
    vertices: Set[VertexT]
  ): Map[VertexT, Set[VertexT]] = {
    val verticesWithConsistentOrder = vertices.toList
    verticesWithConsistentOrder
      .map { vertex =>
        graph
          .incomingEdgesOf(vertex)
          .asScala
          .toSet
          .map(e => graph.getEdgeSource(e))
      }
      .zip(verticesWithConsistentOrder)
      .filter(_._1.size > 1)
      .map(_.swap)
      .toMap
  }

  case class ParentWithDistance(parent: VertexT, distanceFromChild: Int)

  /** Finds a vertex that could be the common ancestor of the supplied vertex
    * and its cousins, or returns None. See the documentation of the containing
    * object for examples of how this function works.
    */
  @tailrec
  private def findPotentialCommonAncestor(
    graph : GraphT,
    vertex: VertexT,
    depth : Int = 0
  ): Option[ParentWithDistance] = {
    // If the current vertex has multiple children, we've found our target.
    if (graph.outDegreeOf(vertex) > 1) {
      Some(ParentWithDistance(vertex, depth))
    }
    else {
      // If we find a vertex that has a single child but multiple parents, then
      // it is not the common ancestor of the supplied vertex and its cousins.
      val incoming = graph.incomingEdgesOf(vertex)
      if (incoming.size != 1) {
        None
      }
      else {
        // If there is a single parent, try that one.
        findPotentialCommonAncestor(
          graph,
          graph.getEdgeSource(incoming.asScala.head),
          depth + 1
        )
      }
    }
  }

  /** When passed a map of non-anonymous vertices to their direct anonymous
    * parents, replaces the set of direct anonymous parents. Each element will
    * be replaced with a direct ancestor that could be a common ancestor of
    * multiple elements of the set. If none are found, the element is removed.
    * If any of the sets are left with fewer than 2 elements, they are removed.
    */
  private def findCommonAncestorOfMergeableParents(
    graph                   : GraphT,
    verticesAndDirectParents: Map[VertexT, Set[VertexT]]
  ): Map[VertexT, Set[ParentWithDistance]] = {
    verticesAndDirectParents
      .map { case (vertex, directParents) =>
        (
          vertex,
          directParents.flatMap(findPotentialCommonAncestor(graph, _))
        )
      }
  }

  /** Should be passed a map of non-anonymous vertices to a set of the potential
    * nearest common ancestors of a number of its direct anonymous parents.
    * (see `findCommonAncestorOfMergeableParents`).
    *
    * Returns a map of the non-anonymous vertices to a collection of paths from
    * the provided parents to the key vertex. Uses the distance from the
    * `ParentWithDistance` passed as a depth limit for the path search.
    *
    * TODO: I can remove the outer Map from this point, and probably get rid of
    * some of the inner map from around here as well.
    */
  private def getPathFromCommonAncestorsToVertex(
    graph               : GraphT,
    verticesAndAncestors: Map[VertexT, Set[ParentWithDistance]]
  ): Map[VertexT, Map[ParentWithDistance, Iterable[GraphPath[VertexT, EdgeT]]]] = {
    val allPathsFinder = new AllDirectedPaths(graph)
    verticesAndAncestors.map { case (vertex, ancestors) =>
      (
        vertex,
        ancestors.map { ancestor =>
          (
            ancestor,
            allPathsFinder.getAllPaths(
              ancestor.parent,
              vertex,
              true,
              ancestor.distanceFromChild + 3
            ).asScala
          )
        }.toMap
      )
    }
  }

  /** Removes paths which contain non-anonymous vertices. This does not consider
    * the start or end vertices of the path, since we know the end is
    * non-anonymous, and it doesn't matter if the start is.
    */
  private def filterRelevantPaths(
    verticesAndPaths: Map[VertexT, Map[ParentWithDistance, Iterable[GraphPath[VertexT, EdgeT]]]]
  ): Map[VertexT, Map[ParentWithDistance, Iterable[GraphPath[VertexT, EdgeT]]]] = {
    verticesAndPaths.map { case (vertex, mapWithPaths) =>
      (
        vertex,
        mapWithPaths.map { case (ancestor, paths) =>
          (
            ancestor,
            paths.filter { path =>
              path
                .getVertexList
                .asScala
                .drop(1)
                .dropRight(1)
                .forall(isAnonymous)
            }
          )
        }
      )
    }
  }

  /** Returns a map of non-anonymous vertices to a second map. The second map
    * is from potential common ancestors of the direct parents of the non-
    * anonymous vertices to a third map. The third map is a collection of paths
    * from the potential common ancestor to the non-anonymous vertex, where the
    * key is the length of the path. This is a map just to make it explicit that
    * they're grouped by length. We also discard any groups with a single path.
    */
  private def groupPathsByLength(
    verticesAndPaths: Map[VertexT, Map[ParentWithDistance, Iterable[GraphPath[VertexT, EdgeT]]]]
  ): Map[VertexT, Map[ParentWithDistance, Map[Int, Iterable[GraphPath[VertexT, EdgeT]]]]] = {
    verticesAndPaths
      .map { case (vertex, ancestorAndPaths) =>
        (
          vertex,
          ancestorAndPaths
            .map { case (ancestor, paths) =>
              (
                ancestor,
                paths
                  .groupBy(_.getLength)
                  .filter(_._2.size > 1)
              )
            }
        )
      }
  }

  /** Returns a collection of vertices to merge. */
  private def getVerticesToMerge(
    verticesAndGroupedPaths: Map[VertexT, Map[ParentWithDistance, Map[Int, Iterable[GraphPath[VertexT, EdgeT]]]]]
  ): Iterable[Iterable[Iterable[VertexT]]] = {
    verticesAndGroupedPaths
      .flatMap { case (_, ancestorAndPaths) =>
        ancestorAndPaths.flatMap { case (_, groupedPaths) =>
          groupedPaths
            .map { group =>
              group._2.map { path =>
                path.getVertexList.asScala.drop(1).dropRight(1)
              }.transpose
            }
        }
      }
  }

  def getVerticesToMerge(
    graph: GraphT
  ): Iterable[Iterable[Iterable[VertexT]]] = {
    val nonAnonymousVertices = getNonAnonymousVertices(graph)
    val verticesWithMultipleAnonymousParents = filterVerticesWithMultipleAnonymousParents(graph, nonAnonymousVertices)
    val verticesWithPotentialCommonAncestors = findCommonAncestorOfMergeableParents(graph, verticesWithMultipleAnonymousParents)
    val verticesAndPathsFromParents = getPathFromCommonAncestorsToVertex(graph, verticesWithPotentialCommonAncestors)
    val onlyRelevantPaths = filterRelevantPaths(verticesAndPathsFromParents)
    val pathsGroupedByLength = groupPathsByLength(onlyRelevantPaths)
    val hostsToMerge = getVerticesToMerge(pathsGroupedByLength)
    hostsToMerge
  }
}
