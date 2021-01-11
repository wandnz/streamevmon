package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.connectors.postgres.schema.AsInetPath
import nz.net.wand.streamevmon.events.grouping.graph.pruning.{GraphPruneLastSeenTime, GraphPruneParallelAnonymousHost}

import java.time.{Duration, Instant}

import org.jgrapht.graph.DefaultDirectedWeightedGraph

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
    new GraphPruneParallelAnonymousHost[VertexT, EdgeT, GraphT]().prune(graph)
  }

  /** Prunes edges that are older than the configured time (`pruneAge`), and
    * removes any vertices that are no longer connected to the rest of the graph.
    */
  def pruneGraphByLastSeenTime(graph: GraphT, pruneAge: Duration, currentTime: Instant): Unit = {
    new GraphPruneLastSeenTime[VertexT, EdgeT, GraphT](pruneAge, currentTime).prune(graph)
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
