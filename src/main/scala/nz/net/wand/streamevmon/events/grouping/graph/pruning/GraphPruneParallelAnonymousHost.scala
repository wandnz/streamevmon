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

import nz.net.wand.streamevmon.events.grouping.graph.building.GraphChangeEvent.MergeVertices
import nz.net.wand.streamevmon.events.grouping.graph.impl.GraphType._

import org.jgrapht.GraphPath
import org.jgrapht.alg.shortestpath.AllDirectedPaths

import scala.annotation.tailrec
import scala.collection.JavaConverters._

/** Merges probable duplicate anonymous hosts by finding parallel paths of
  * anonymous hosts with the same length.
  *
  * If a host does not respond to a traceroute query, it is represented as an
  * anonymous host, which uses its location in a traceroute path to distinguish
  * itself. If one of these anonymous hosts is part of multiple AMP traceroute
  * paths, it will appear once in each of these paths.
  *
  * This results in cases where an identified host has many children, each of
  * which has only one child, making a group of long straight parallel lines
  * containing many hosts. Eventually, the lines will all converge to a single
  * identified host. These parallel paths are very likely to be the same set
  * of hosts, so we want to merge them into a single path.
  *
  * The rest of this section provides documentation from the perspective of
  * describing how the private function `findPotentialCommonAncestor()` works.
  *
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
  * from `a` to `g` via `d` and `e` respectively. As such, calling the
  * function on `d` will return None, indicating that it should not be merged.
  */
object GraphPruneParallelAnonymousHost {

  private def isAnonymous(vertex: VertexT): Boolean = vertex.ampTracerouteUids.nonEmpty

  /** Gets all the non-anonymous vertices from the graph.
    */
  private def getNonAnonymousVertices(graph: GraphT): Iterable[VertexT] = graph
    .vertexSet
    .asScala
    .filter(!isAnonymous(_))

  /** Removes those non-anonymous vertices which do not have multiple anonymous
    * direct parents. Returns a map of the non-anonymous vertex to a set of
    * its anonymous parents. The size of the set will be greater than 1.
    */
  private def filterVerticesWithMultipleAnonymousParents(
    graph: GraphT,
    vertices: Iterable[VertexT]
  ): Map[VertexT, Iterable[VertexT]] = {
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
    verticesAndDirectParents: Map[VertexT, Iterable[VertexT]]
  ): Map[VertexT, Iterable[ParentWithDistance]] = {
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
    * Returns a collection of paths from the provided parents to their
    * corresponding non-anonymous vertex. Uses the distance from the
    * `ParentWithDistance` as a depth limit for the path search.
    */
  private def getPathsFromCommonAncestorsToVertex(
    graph      : GraphT,
    verticesAndAncestors: Map[VertexT, Iterable[ParentWithDistance]]
  ): Iterable[GraphPath[VertexT, EdgeT]] = {
    val allPathsFinder = new AllDirectedPaths(graph)
    verticesAndAncestors.flatMap { case (vertex, ancestors) =>
      ancestors.flatMap { ancestor =>
        allPathsFinder.getAllPaths(
          ancestor.parent,
          vertex,
          true,
          ancestor.distanceFromChild + 3
        ).asScala
      }
    }
  }

  /** Removes paths which contain non-anonymous vertices. This does not consider
    * the start or end vertices of the path, since we know the end is
    * non-anonymous, and it doesn't matter if the start is.
    */
  private def filterRelevantPaths(
    paths: Iterable[GraphPath[VertexT, EdgeT]]
  ): Iterable[GraphPath[VertexT, EdgeT]] = {
    paths.filter { path =>
      path
        .getVertexList
        .asScala
        .drop(1)
        .dropRight(1)
        .forall(isAnonymous)
    }
  }

  /** Given a collection of paths, finds those paths which are parallel and
    * have the same length. Returns a collection containing those groups of
    * paths.
    */
  private def groupSiblingPaths(
    paths: Iterable[GraphPath[VertexT, EdgeT]]
  ): Iterable[Iterable[GraphPath[VertexT, EdgeT]]] = {
    paths
      .groupBy(path => (path.getLength, path.getStartVertex, path.getEndVertex))
      .filter(_._2.size > 1)
      .values
  }

  /** Returns a collection of groups of vertices that can be merged. */
  private def getVerticesToMerge(
    groupedPaths: Iterable[Iterable[GraphPath[VertexT, EdgeT]]]
  ): Iterable[Iterable[VertexT]] = {
    groupedPaths
      .flatMap { group =>
        group.map { path =>
          path.getVertexList.asScala.drop(1).dropRight(1)
        }.transpose
      }
  }

  /** Given a graph, returns a collection of groups of vertices that can be
    * merged.
    */
  def getMergeVertices(
    graph: GraphT
  ): Iterable[MergeVertices] = {
    val nonAnonymousVertices = getNonAnonymousVertices(graph)
    val verticesWithMultipleAnonymousParents = filterVerticesWithMultipleAnonymousParents(graph, nonAnonymousVertices)
    val verticesWithPotentialCommonAncestors = findCommonAncestorOfMergeableParents(graph, verticesWithMultipleAnonymousParents)
    val verticesAndPathsFromParents = getPathsFromCommonAncestorsToVertex(graph, verticesWithPotentialCommonAncestors)
    val onlyRelevantPaths = filterRelevantPaths(verticesAndPathsFromParents)
    val pathsGroupedByLength = groupSiblingPaths(onlyRelevantPaths)
    val hostsToMerge = getVerticesToMerge(pathsGroupedByLength)
    hostsToMerge.map(hosts => MergeVertices(hosts.toSet))
  }
}
