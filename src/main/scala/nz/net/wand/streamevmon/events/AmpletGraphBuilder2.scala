package nz.net.wand.streamevmon.events

import nz.net.wand.streamevmon.connectors.postgres.AsInetPath

import org.jgrapht.graph.{DefaultDirectedWeightedGraph, DefaultWeightedEdge, GraphWalk}

import scala.collection.JavaConverters._
import scala.collection.mutable

object AmpletGraphBuilder2 {

  type VertexT = Host
  type EdgeT = DefaultWeightedEdge
  type GraphT = DefaultDirectedWeightedGraph[VertexT, EdgeT]

  /** Annotates an AsInetPath with Host objects for each hop in the path.
    */
  def pathsToHosts(paths: Iterable[AsInetPath]): Iterable[(AsInetPath, Iterable[Host])] = {
    paths.map(path => (path,
      path.zipWithIndex.map { case (entry, index) =>
        // We can usually extract a hostname for the source and destination of
        // the test. This might prevent later host merges if there are two tests
        // for the same destination, but one specified a hostname and the other
        // an IP.
        val `head` = path.head
        val `tail` = path.last
        val hostname = entry match {
          case `head` => Some(path.meta.source)
          case `tail` => Some(path.meta.destination)
          case _ => None
        }

        (hostname, entry.address) match {
          case (Some(host), _) => new HostWithKnownHostname(
            // This maps from an Option[InetAddress] to an Option[(InetAddress, AsNumber)].
            // Since an Option is an Iterable, this is fine. It'll get turned into
            // a regular seq-like collection later if it needs to be.
            host, entry.address.map(addr => (addr, entry.as))
          )
          case (None, Some(addr)) => new HostWithUnknownHostname((addr, entry.as))
          case (None, None) => new HostWithUnknownAddress(path.meta.stream, path.measurement.path_id, index)
        }
      }
    ))
  }

  def buildGraph(paths: Iterable[AsInetPath]): GraphT = {
    val graph = new GraphT(classOf[EdgeT])

    // First, we want to annotate all the hops in all our paths with some initial
    // Host objects, which is what we use as graph nodes.
    val pathsAndHosts = pathsToHosts(paths)

    // We make a lookup table to convert Host objects into their merged forms.
    // This is a kind of duplicate-squishing, but it collates hosts with multiple
    // known IP addresses for a single hostname.
    val mergedHostLookupTable = pathsAndHosts
      // Get a flat list of all the known Hosts
      .flatMap(_._2)
      // Include their UID as the left side of a tuple including the host
      .map(h => h.uid -> h)
      // Group by the UID - this makes a Map, where the key is the UID and the
      // value is a list of hosts matching that key
      .groupBy(_._1)
      // Extract the host so the value doesn't also include the key, and then
      // merge all the hosts that share UIDs.
      .mapValues { hosts => hosts.map(_._2).reduce((a, b) => a.mergeWith(b)) }

    val knownPathLookupTable = mutable.Map[Host, Iterable[GraphWalk[VertexT, EdgeT]]]()

    // Now, let's put all the paths into the graph we made earlier.
    pathsAndHosts.foreach { case (path, hosts) =>
      graph.addVertex(mergedHostLookupTable(hosts.head.uid))
      if (path.size > 1) {
        // This adds the hosts and edges to the graph, and returns the edges.
        val edges = path
          .zip(hosts)
          .sliding(2).map { elems =>
          val source = elems.head
          val dest = elems.last

          graph.addVertex(mergedHostLookupTable(dest._2.uid))
          graph.addEdge(
            mergedHostLookupTable(source._2.uid),
            mergedHostLookupTable(dest._2.uid)
          )
        }

        // Turn the edges into a GraphWalk
        val walk = new GraphWalk[VertexT, EdgeT](
          graph,
          mergedHostLookupTable(hosts.head.uid),
          mergedHostLookupTable(hosts.last.uid),
          edges.toList.asJava,
          0.0
        )

        // Write down that the walk applies to each of the vertices it visits
        hosts.foreach { h =>
          if (knownPathLookupTable.contains(h)) {
            knownPathLookupTable(h) = knownPathLookupTable(h).toList :+ walk
          }
          else {
            knownPathLookupTable(h) = List(walk)
          }
        }
      }
    }

    // Attach the walks to their hosts
    graph.vertexSet.forEach { v =>
      v.knownPaths ++= knownPathLookupTable(v)
    }

    graph
  }
}
