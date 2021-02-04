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

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.connectors.postgres.schema.AsInetPath
import nz.net.wand.streamevmon.flink.HasFlinkConfig

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.util.Collector
import org.jgrapht.graph.{DefaultDirectedWeightedGraph, DefaultWeightedEdge}

import scala.collection.mutable
import scala.collection.JavaConverters._

/** Attempts to place events on a topological network graph.
  *
  * The first input is the stream of new events which should be grouped.
  *
  * The second input is a stream of new AsInetPaths which are generated from
  * Traceroute measurements. Duplicates are acceptable,
  *
  * Currently, the "placing events" functionality is unimplemented, and this
  * just acts as a passthrough function for Events that takes up a lot of memory.
  */
class TraceroutePathGraph[EventT <: Event]
  extends CoProcessFunction[EventT, AsInetPath, Event]
          with CheckpointedFunction
          with HasFlinkConfig
          with Logging {
  override val flinkName: String = "Traceroute-Path Graph"
  override val flinkUid: String = "traceroute-path-graph"
  override val configKeyGroup: String = "no-config"

  type VertexT = Host
  type EdgeT = DefaultWeightedEdge
  type GraphT = DefaultDirectedWeightedGraph[VertexT, EdgeT]

  var graph: GraphT = _

  /** Multiple hosts that share attributes can be merged into a single host that
    * contains multiple addresses. This is a lookup table from the source host
    * UID to the merged version of that host.
    *
    * // TODO: What happens here if we have two separate source hosts that get
    * merged? Does the map get updated properly? Maybe we need to look at
    * a different data structure that is better at storing multiple keys
    * that point to a single value.
    */
  val mergedHosts: mutable.Map[String, VertexT] = mutable.Map()

  override def open(parameters: Configuration): Unit = {
    // We can't go sharing inputs with other parallel instances, since that
    // would mean everyone has an incomplete graph.
    if (getRuntimeContext.getNumberOfParallelSubtasks > 1) {
      throw new IllegalStateException("Parallelism for this CoProcessFunction must be 1.")
    }
  }

  override def processElement1(
    value: EventT,
    ctx: CoProcessFunction[EventT, AsInetPath, Event]#Context,
    out  : Collector[Event]
  ): Unit = {
    out.collect(value)
  }

  /** Converts an AsInetPath into a path of Hosts. */
  def pathToHosts(path: AsInetPath): Iterable[Host] = {
    path.zipWithIndex.map { case (entry, index) =>
      // We can usually extract a hostname for the source and destination of
      // the test from the metadata.
      val lastIndex = path.size - 1
      val hostname = index match {
        case 0 => Some(path.meta.source)
        case i if i == lastIndex => Some(path.meta.destination)
        case _ => None
      }

      (hostname, entry.address) match {
        case (Some(host), _) => new HostWithKnownHostname(host, entry.address.map(addr => (addr, entry.as)).toSet)
        case (None, Some(addr)) => new HostWithUnknownHostname((addr, entry.as))
        case (None, None) => new HostWithUnknownAddress(path.meta.stream, path.measurement.path_id, index)
      }
    }
  }

  /** Replaces a vertex in a GraphT with a new vertex, retaining connected edges.
    *
    * Originally from https://stackoverflow.com/a/48255973, but needed some
    * additional changes to work with our equality definition for Hosts.
    */
  def replaceVertex(graph: GraphT, oldHost: VertexT, newHost: VertexT): Unit = {
    if (!oldHost.deepEquals(newHost)) {
      val outEdges = graph.outgoingEdgesOf(oldHost).asScala.map(edge => (graph.getEdgeTarget(edge), edge))
      val inEdges = graph.incomingEdgesOf(oldHost).asScala.map(edge => (graph.getEdgeSource(edge), edge))
      graph.removeVertex(oldHost)
      graph.addVertex(newHost)
      outEdges.foreach(edge => graph.addEdge(newHost, edge._1, edge._2))
      inEdges.foreach(edge => graph.addEdge(edge._1, newHost, edge._2))
    }
  }

  /** Adds an AsInetPath to the graph. New hosts will become new vertices, and
    * missing edges will be added. Gives no output.
    */
  override def processElement2(
    value: AsInetPath,
    ctx  : CoProcessFunction[EventT, AsInetPath, Event]#Context,
    out  : Collector[Event]
  ): Unit = {
    // First, let's convert the AsInetPath to a collection of Host hops.
    val hosts = pathToHosts(value)

    // For each Host, replace it with the deduplicated equivalent.
    hosts
      .foreach { h =>
        // Put the deduplicated entry back into the map with UID as key.
        mergedHosts.put(
          h.uid,
          // We get it by taking the existing entry with a matching UID...
          mergedHosts
            .get(h.uid)
            // ... and merging it with the new entry. We also need to update
            // the entry in the graph here.
            .map { oldMerged =>
              val newMerged = oldMerged.mergeWith(h)
              replaceVertex(graph, oldMerged, newMerged)
              newMerged
            }
            // If there wasn't an existing entry in the first place, we just put
            // the new entry there instead. Also slap it into the graph.
            .getOrElse {
              graph.addVertex(h)
              h
            }
        )
      }

    // Add the new edges representing the new path to the graph.
    // We do a zip here so that we have the option of creating GraphWalks that
    // also represent the paths later. If we do choose to implement that, note
    // that serializing a GraphWalk to send it via Flink implies serializing
    // the entire graph!
    value
      .zip(hosts)
      .sliding(2)
      .foreach { elems =>
        val source = elems.head
        val dest = elems.lastOption
        dest.foreach { d =>
          graph.addEdge(
            mergedHosts(source._2.uid),
            mergedHosts(d._2.uid)
          )
        }
      }
  }

  // == CheckpointedFunction implementation ==

  private var graphState: ListState[GraphT] = _
  private var mergedHostsState: ListState[VertexT] = _

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    graphState.clear()
    graphState.add(graph)
    mergedHostsState.clear()
    mergedHostsState.addAll(mergedHosts.values.toSeq.asJava)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    graphState = context
      .getOperatorStateStore
      .getUnionListState(new ListStateDescriptor("graph", classOf[GraphT]))

    mergedHostsState = context
      .getOperatorStateStore
      .getUnionListState(new ListStateDescriptor("mergedHosts", classOf[VertexT]))

    if (context.isRestored) {
      graphState.get.forEach(entry => graph = entry)
      mergedHostsState.get.forEach(entry => mergedHosts.put(entry.uid, entry))
    }
    else {
      graph = new GraphT(classOf[EdgeT])
      graph.setEdgeSupplier(new DefaultWeightedEdgeSupplier)
    }
  }
}
