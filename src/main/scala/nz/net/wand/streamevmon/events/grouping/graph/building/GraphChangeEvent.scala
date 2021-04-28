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

import nz.net.wand.streamevmon.events.grouping.graph.impl.GraphType._
import nz.net.wand.streamevmon.events.grouping.graph.pruning.GraphPruneParallelAnonymousHost

import java.time.Instant

import org.jgrapht.alg.connectivity.ConnectivityInspector

import scala.collection.JavaConverters._

/** Parent class for change events. All they need to do is apply themselves to
  * the graph they're given.
  */
sealed abstract class GraphChangeEvent {
  def apply(graph: GraphT): GraphT = {
    applyInternal(graph)
    graph
  }

  protected def applyInternal(graph: GraphT): Unit
}

/** Declares various types of graph change event. Each of them should apply a
  * simple operation to an existing graph, and should be fully self-contained.
  */
object GraphChangeEvent {
  case class AddVertex(vertex: VertexT) extends GraphChangeEvent {
    override protected def applyInternal(graph: GraphT): Unit = graph.addVertex(vertex)
  }

  case class RemoveVertex(vertex: VertexT) extends GraphChangeEvent {
    override protected def applyInternal(graph: GraphT): Unit = graph.removeVertex(vertex)
  }

  /** Originally from https://stackoverflow.com/a/48255973, but needed some
    * additional changes to work with our equality definition for Hosts.
    */
  case class UpdateVertex(before: VertexT, after: VertexT) extends GraphChangeEvent {
    override protected def applyInternal(graph: GraphT): Unit = {
      val outEdges = graph.outgoingEdgesOf(before).asScala.map(edge => (graph.getEdgeTarget(edge), edge))
      val inEdges = graph.incomingEdgesOf(before).asScala.map(edge => (graph.getEdgeSource(edge), edge))
      graph.removeVertex(before)
      graph.addVertex(after)

      // If any of the edges are connected to either the old host or the new
      // host on both sides, then we're creating a self-loop. We opt to
      // drop them, since they're not useful in determining a network topology.
      outEdges
        .filterNot(e => e._1 == before || e._1 == after)
        .foreach(edge => graph.addEdge(after, edge._1, edge._2))
      inEdges
        .filterNot(e => e._1 == before || e._1 == after)
        .foreach(edge => graph.addEdge(edge._1, after, edge._2))
    }
  }

  /** UpdateVertex is a little unusual in that it might do nothing if the two
    * arguments are the same. We display this behaviour using the type system
    * if the caller wants the additional information, but they can also just
    * get a plain GraphChangeEvent, which could be either.
    */
  object UpdateVertex {
    def apply(before: VertexT, after: VertexT): Either[DoNothing, UpdateVertex] = {
      if (before == after) {
        Left(DoNothing())
      }
      else {
        Right(new UpdateVertex(before, after))
      }
    }

    def create(before: VertexT, after: VertexT): GraphChangeEvent = {
      apply(before, after).fold(l => l, r => r)
    }
  }

  case class MergeVertices(vertices: Iterable[VertexT]) extends GraphChangeEvent {
    lazy val merged: VertexT = vertices
      .drop(1)
      .foldLeft(vertices.head)((a, b) => a.mergeWith(b, forceMergeAnonymousHosts = true))

    override protected def applyInternal(graph: GraphT): Unit = {
      vertices.foreach(v => UpdateVertex.create(v, merged).apply(graph))
    }
  }

  case class AddOrUpdateEdge(start: VertexT, end: VertexT, edge: EdgeT) extends GraphChangeEvent {
    override protected def applyInternal(graph: GraphT): Unit = {
      val oldEdge = graph.getEdge(start, end)
      if (oldEdge != null) {
        graph.removeEdge(oldEdge)
      }
      graph.addEdge(start, end, edge)
    }
  }

  case class RemoveEdge(edge: EdgeT) extends GraphChangeEvent {
    override protected def applyInternal(graph: GraphT): Unit = graph.removeEdge(edge)
  }

  case class RemoveEdgeByVertices(start: VertexT, end: VertexT) extends GraphChangeEvent {
    override protected def applyInternal(graph: GraphT): Unit = graph.removeEdge(start, end)
  }

  /** Meta-event that marks the end of an input measurement to the graph
    * building system. Some downstream graph-building operators want to know
    * how many measurements have been processed, so this lets them keep count.
    */
  case class MeasurementEndMarker(timeOfMeasurement: Instant) extends GraphChangeEvent {
    override protected def applyInternal(graph: GraphT): Unit = {}
  }

  case class RemoveOldEdges(ageCutoff: Instant) extends GraphChangeEvent {
    override protected def applyInternal(graph: GraphT): Unit = {
      graph
        .edgeSet
        .asScala
        .toList
        .foreach { edge =>
          if (edge.lastSeen.isBefore(ageCutoff)) {
            RemoveEdge(edge).apply(graph)
          }
        }
    }
  }

  sealed abstract class NoArgumentGraphChangeEvent extends GraphChangeEvent

  case class DoNothing() extends NoArgumentGraphChangeEvent {
    override protected def applyInternal(graph: GraphT): Unit = {}
  }

  case class RemoveUnconnectedVertices() extends NoArgumentGraphChangeEvent {
    override protected def applyInternal(graph: GraphT): Unit = {
      graph.removeAllVertices(
        new ConnectivityInspector(graph)
          .connectedSets
          .asScala
          .sortBy(_.size)
          .dropRight(1)
          .flatMap(_.asScala)
          .toSet
          .asJavaCollection
      )
    }
  }

  /** This is a complex algorithm, but its effects can still be broken down into
    * a series of basic components. We allow users to get the component events
    * separately, since there are cases where we need to pass them around
    * with references to the vertices they affect, which this class does not
    * provide alone.
    */
  case class DoPruneParallelAnonymousHosts() extends NoArgumentGraphChangeEvent {
    def getMergeVertexEvents(graph: GraphT): Iterable[MergeVertices] = {
      GraphPruneParallelAnonymousHost.getMergeVertices(graph)
    }

    def applyMergeVertexEvents(graph: GraphT, events: Iterable[MergeVertices]): Unit = {
      events.foreach(_.apply(graph))
    }

    override protected def applyInternal(graph: GraphT): Unit = {
      applyMergeVertexEvents(graph, getMergeVertexEvents(graph))
    }
  }
}
