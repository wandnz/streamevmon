package nz.net.wand.streamevmon

import nz.net.wand.streamevmon.connectors.postgres.{AsInetPath, AsInetPathEntry, AsNumberCategory}

import java.io.File

import org.jgrapht.graph.{DefaultDirectedWeightedGraph, DefaultWeightedEdge}
import org.jgrapht.nio.dot.DOTExporter

import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`

class TracerouteImplPlayground extends PostgresContainerSpec {

  "Traceroute" should {
    "work" in {

      val graph = new DefaultDirectedWeightedGraph[AsInetPathEntry, DefaultWeightedEdge](classOf[DefaultWeightedEdge])

      // Let's make a graph of all the traceroute hops we know about
      getPostgres.getAllTracerouteMeta match {
        case None => fail()
        // For every traceroute stream
        case Some(metas) => metas.foreach { meta =>
          // Get all the traceroute measurements to discover the paths that are
          // in use for the data we care about. This also keeps the pairs of
          // path:aspath together, which is necessary for pairing up InetAddresses
          // to their ASN.
          val entries = getPostgres.getTracerouteData(meta.stream).get

          // Tie together the InetPath and AsPath, and get the unique ones.
          val paths = entries.map { entry =>
            val path = getPostgres.getTraceroutePath(entry)
            val asPath = getPostgres.getTracerouteAsPath(entry)

            AsInetPath(path.get.path, asPath.map(_.aspath))
          }.toSet

          paths.foreach { path =>
            // For every path, we add every hop as a vertex
            path.foreach { hop =>
              graph.addVertex(hop)
            }
            // And we add every edge between hops as an edge
            path.sliding(2).foreach { pairs =>
              graph.addEdge(pairs.head, pairs.last)
            }
          }
        }
      }

      // Now that we have a big graph, we might have some duplicate vertices.
      // We want to merge these in such a way that we retain AS information.
      graph.vertexSet.toList.flatMap { node =>
        // If a node has an AS number attached...
        if (node.as.category != AsNumberCategory.Missing) {
          // Find any other nodes with the same inet address but missing ASNs.
          // We'll output all of our nodes-to-be-replaced as a new list of
          // tuples with what they should be replaced with.
          graph.vertexSet.filter { n =>
            n.address == node.address && n.as.category == AsNumberCategory.Missing
          }.map((_, node))
        }
        // If this node is missing data, just skip it. We won't be replacing
        // any other nodes with it.
        else {
          Seq()
        }
      }.foreach { case (toBeReplaced, newNode) =>
        // Let's go ahead and replace all the nodes we wrote about earlier.
        // We need to keep track of the edges in and out of the node we're
        // about to remove, so we can add them back later.
        val inEdges = graph.incomingEdgesOf(toBeReplaced).toList
        val outEdges = graph.outgoingEdgesOf(toBeReplaced).toList

        // Swap the nodes out.
        graph.removeVertex(toBeReplaced)
        graph.addVertex(newNode)
        // Put all the edges back in, pointing to the new node.
        inEdges.foreach(edge => graph.addEdge(graph.getEdgeSource(edge), newNode, edge))
        outEdges.foreach(edge => graph.addEdge(newNode, graph.getEdgeTarget(edge), edge))
      }

      // Let's print this graph to a .dot file so we can draw it later.
      val exporter = new DOTExporter[AsInetPathEntry, DefaultWeightedEdge]()

      exporter.setVertexIdProvider(entry => s""" "${entry.toString}" """.trim)

      exporter.exportGraph(graph, new File("out/traceroute.dot"))

      println(graph)
    }
  }
}
