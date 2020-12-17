package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.connectors.postgres.schema.AsInetPath

import java.time.{Duration, Instant}

import org.jgrapht.alg.connectivity.ConnectivityInspector
import org.jgrapht.alg.shortestpath.AllDirectedPaths
import org.jgrapht.graph.DefaultDirectedWeightedGraph

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.JavaConverters._

trait GraphConstructionLogic extends Logging {
  type VertexT = Host
  type EdgeT = EdgeWithLastSeen
  type GraphT = DefaultDirectedWeightedGraph[VertexT, EdgeT]

  def getMergedHosts: mutable.Map[String, VertexT]

  var lastPruneTime: Instant = Instant.EPOCH
  var measurementsSinceLastPrune: Long = 0

  /** If there are multiple parallel paths between two hosts with the same length
    * that are solely composed of anonymous hosts, then it's likely that they're
    * the same hosts each time, and meaningless to retain the information of how
    * many separate traceroute paths took that route. This function merges that
    * kind of duplicate host group.
    *
    * TODO: Implement the actual merge functionality once Host supports it (or
    * we've decided how to represent merged anonymous hosts). We probably also
    * want to split each of the filter / map functions into a separate def so
    * that the logic flow is more readable.
    */
  def pruneGraphByParallelAnonymousHostPathMerge(graph: GraphT): Unit = {
    val allPaths = new AllDirectedPaths(graph)
    graph
      .vertexSet
      .asScala
      // Find all non-anonymous vertices...
      .filter(_.ampTracerouteUid.isEmpty)
      // where there is more than one edge with that vertex as its target...
      .filter { v =>
        graph
          .incomingEdgesOf(v)
          .asScala
          .count { e =>
            // that has an anonymous host on the other end.
            graph
              .getEdgeSource(e)
              .ampTracerouteUid
              .isDefined
          } > 1
      }
      // Retrieve the anonymous hosts that are the direct parents of this relevant host
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
      // Determine the nearest parent on each path upward that has multiple children.
      .map { case (bottomHost, directParents) =>
        @tailrec
        def findDirectParentWithMultipleChildren(vertex: VertexT): Option[VertexT] = {
          // If the current node has multiple children, we've found our target.
          if (graph.outDegreeOf(vertex) > 1) {
            Some(vertex)
          }
          else {
            // If it doesn't have multiple children, but it doesn't have only a single
            // parent, then what we're looking for doesn't exist.
            // If there are no parents, we can't look any further.
            // If there are multiple parents, there isn't a "direct" parent and this
            // part of the graph is the wrong shape for what we're expecting.
            val incoming = graph.incomingEdgesOf(vertex)
            if (incoming.size != 1) {
              None
            }
            else {
              // If there is a single parent, try that one.
              findDirectParentWithMultipleChildren(graph.getEdgeSource(incoming.asScala.head))
            }
          }
        }

        (
          bottomHost,
          directParents.flatMap(findDirectParentWithMultipleChildren)
        )
      }
      // Find all the paths between the shared parents and the bottom host.
      .map { case (bottomHost, sharedParents) =>
        (
          bottomHost,
          sharedParents.map { parent =>
            (
              parent,
              allPaths.getAllPaths(parent, bottomHost, true, null).asScala
            )
          }
            .toMap
        )
      }
      // Filter any paths which don't strictly contain anonymous hosts
      .map { case (bottomHost, mapTopHostToPaths) =>
        (
          bottomHost,
          mapTopHostToPaths.map { case (topHost, paths) =>
            (
              topHost,
              paths.filter { path =>
                path
                  .getVertexList
                  .asScala
                  .drop(1)
                  .dropRight(1)
                  .forall(_.ampTracerouteUid.isDefined)
              }
            )
          }
        )
      }
      // Group paths by their length, and filter any that have a unique length
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
  }

  /** Prunes edges that are older than the configured time (`pruneAge`), and
    * removes any vertices that are no longer connected to the rest of the graph.
    */
  def pruneGraphByLastSeenTime(graph: GraphT, pruneAge: Duration, currentTime: Instant): Unit = {
    val startTime = System.nanoTime()

    graph
      .edgeSet
      .asScala
      // convert the edge list to a solid list instead of a lazy collection so
      // that we're not modifying the graph during an iteration over its edges.
      .toList
      .foreach { edge =>
        // if it's old enough, get rid of it.
        if (Duration.between(edge.lastSeen, currentTime).compareTo(pruneAge) > 0) {
          graph.removeEdge(edge)
        }
      }

    // The connectivity inspector can tell us about all sets of connected
    // elements within the graph. We preserve the one with the most elements,
    // since it's the main part of the graph that shouldn't be pruned.
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

    val endTime = System.nanoTime()
    logger.trace(s"Pruning graph took ${Duration.ofNanos(endTime - startTime).toMillis}ms")
    lastPruneTime = currentTime
    measurementsSinceLastPrune = 0
  }

  /** Converts an AsInetPath into a path of Hosts. */
  def pathToHosts(path: AsInetPath): Iterable[VertexT] = {
    path.zipWithIndex.map { case (entry, index) =>
      // We can usually extract a hostname for the source and destination of
      // the test from the metadata.
      val lastIndex = path.size - 1
      val hostname = index match {
        case 0 => Some(path.meta.source)
        case i if i == lastIndex => Some(path.meta.destination)
        case _ => None
      }

      val hostnames = hostname.toSet
      val addresses = entry.address.map(addr => (addr, entry.as)).toSet
      Host(
        hostnames,
        addresses,
        if (hostnames.isEmpty && addresses.isEmpty) {
          Some((path.meta.stream, path.measurement.path_id, index))
        }
        else {
          None
        },
        None
      )
    }
  }

  /** Replaces a vertex in a GraphT with a new vertex, retaining connected edges.
    * If the original vertex wasn't present, just add the new vertex.
    *
    * Originally from https://stackoverflow.com/a/48255973, but needed some
    * additional changes to work with our equality definition for Hosts.
    */
  def addOrUpdateVertex(graph: GraphT, oldHost: VertexT, newHost: VertexT): Unit = {
    if (graph.containsVertex(oldHost)) {
      if (oldHost != newHost) {
        val outEdges = graph.outgoingEdgesOf(oldHost).asScala.map(edge => (graph.getEdgeTarget(edge), edge))
        val inEdges = graph.incomingEdgesOf(oldHost).asScala.map(edge => (graph.getEdgeSource(edge), edge))
        graph.removeVertex(oldHost)
        graph.addVertex(newHost)

        // If any of the edges are connected to either the old host or the new
        // host on both sides, then we're creating a self-loop. We will opt to
        // drop them, since they're not useful in determining a network topology.
        outEdges
          .filterNot(e => e._1 == oldHost || e._1 == newHost)
          .foreach(edge => graph.addEdge(newHost, edge._1, edge._2))
        inEdges
          .filterNot(e => e._1 == oldHost || e._1 == newHost)
          .foreach(edge => graph.addEdge(edge._1, newHost, edge._2))
      }
    }
    else {
      graph.addVertex(newHost)
    }
  }

  /** If an edge is present, it is replaced with the argument. If not, it is
    * just added.
    */
  def addOrUpdateEdge(graph: GraphT, source: VertexT, destination: VertexT, edge: EdgeT): Unit = {
    val oldEdge = graph.getEdge(source, destination)
    if (oldEdge != null) {
      graph.removeEdge(oldEdge)
    }
    graph.addEdge(source, destination, edge)
  }

  /** Adds an AsInetPath to the graph. */
  def addAsInetPathToGraph(graph: GraphT, aliasResolver: AliasResolver, path: AsInetPath): Unit = {
    // First, let's convert the AsInetPath to a collection of Host hops.
    val hosts = pathToHosts(path)

    val hostsAfterMerge = hosts.map { host =>
      aliasResolver.resolve(
        host,
        h => graph.addVertex(h),
        (oldH, newH) => addOrUpdateVertex(graph, oldH, newH)
      )
    }

    // Add the new edges representing the new path to the graph.
    // We do a zip here so that we have the option of creating GraphWalks that
    // also represent the paths later. If we do choose to implement that, note
    // that serializing a GraphWalk to send it via Flink implies serializing
    // the entire graph!
    val mergedHosts = getMergedHosts
    path
      .zip(hostsAfterMerge)
      .sliding(2)
      .foreach { elems =>
        val source = elems.head
        val dest = elems.drop(1).headOption
        dest.foreach { d =>
          addOrUpdateEdge(
            graph,
            mergedHosts(source._2.uid),
            mergedHosts(d._2.uid),
            new EdgeWithLastSeen(path.measurement.time)
          )
        }
      }
  }
}
