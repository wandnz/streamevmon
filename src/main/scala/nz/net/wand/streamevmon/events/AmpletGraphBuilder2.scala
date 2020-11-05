package nz.net.wand.streamevmon.events

import nz.net.wand.streamevmon.connectors.postgres.AsInetPath

import org.jgrapht.graph.DefaultDirectedWeightedGraph

object AmpletGraphBuilder2 {

  type VertexT = Host
  type EdgeT = TtlEdge
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
    val hosts = pathsToHosts(paths)

    // We make a lookup table to convert Host objects into their merged forms.
    // This is a kind of duplicate-squishing, but it collates hosts with multiple
    // known IP addresses for a single hostname.
    val mergedHostLookupTable = hosts
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

    graph
  }
}
