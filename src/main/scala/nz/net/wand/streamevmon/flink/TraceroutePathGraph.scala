package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.connectors.postgres.AsInetPath
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.events.grouping.graph._

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.util.Collector
import org.jgrapht.graph.{DefaultDirectedWeightedGraph, DefaultWeightedEdge}

import scala.collection.mutable

/** Attempts to place events on a topological network graph.
  *
  * Input 1 is the new events
  * Input 2 is new AsInetPaths which are generated by Traceroute measurements.
  * We happily accept duplicates.
  */
class TraceroutePathGraph[EventT <: Event]
  extends CoProcessFunction[EventT, AsInetPath, Event]
          with HasFlinkConfig
          with Logging {
  override val flinkName: String = "Traceroute-Path Graph"
  override val flinkUid: String = "traceroute-path-graph"
  override val configKeyGroup: String = "no-config"

  type VertexT = Host
  type EdgeT = DefaultWeightedEdge
  type GraphT = DefaultDirectedWeightedGraph[VertexT, EdgeT]

  val graph = new GraphT(classOf[EdgeT])

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

  def pathToHosts(path: AsInetPath): Iterable[Host] = {
    path.zipWithIndex.map { case (entry, index) =>
      // We can usually extract a hostname for the source and destination of
      // the test. This might prevent later host merges if there are two tests
      // for the same destination, but one specified a hostname and the other
      // an IP.
      val lastIndex = path.size - 1
      val hostname = index match {
        case 0 => Some(path.meta.source)
        case i if i == lastIndex => Some(path.meta.destination)
        case _ => None
      }

      (hostname, entry.address) match {
        case (Some(host), _) => new HostWithKnownHostname(host, entry.address.map(addr => (addr, entry.as)))
        case (None, Some(addr)) => new HostWithUnknownHostname((addr, entry.as))
        case (None, None) => new HostWithUnknownAddress(path.meta.stream, path.measurement.path_id, index)
      }
    }
  }

  /** Translated from https://stackoverflow.com/a/48255973 */
  def replaceVertex(graph: GraphT, oldHost: VertexT, newHost: VertexT): Unit = {
    graph.addVertex(newHost)
    graph.outgoingEdgesOf(oldHost).forEach(edge => graph.addEdge(newHost, graph.getEdgeTarget(edge), edge))
    graph.incomingEdgesOf(oldHost).forEach(edge => graph.addEdge(graph.getEdgeSource(edge), newHost, edge))
    graph.removeVertex(oldHost)
  }

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

    counter += 1
    if (counter > 10000) {
      val breakpoint = 1
    }
  }

  var counter = 0
}
