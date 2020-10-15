package nz.net.wand.streamevmon.events

import nz.net.wand.streamevmon.{Caching, Logging}
import nz.net.wand.streamevmon.connectors.postgres._
import nz.net.wand.streamevmon.measurements.amp._

import java.io.File
import java.time.Instant

import org.apache.flink.api.java.utils.ParameterTool
import org.jgrapht.graph.{DefaultDirectedWeightedGraph, DefaultWeightedEdge}

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

/** Builds a graph from traceroute entries in the database connected to via the
  * supplied PostgresConnection.
  *
  * Before `rebuildGraph` is called, `graph` is None. Note that `rebuildGraph`
  * can take a long time depending on the size of the database, and will issue
  * a large number of queries.
  *
  * @param ttl             How long data should remain cached before another call to
  *                        `rebuildCache` re-queries the database.
  * @param memcachedConfig If defined, this class will use the Memcached server
  *                        configured in the ParameterTool. If None (as is the
  *                        default), an in-memory cache is used.
  */
class AmpletGraphBuilder(
  postgres       : PostgresConnection,
  ttl            : Option[FiniteDuration] = None,
  memcachedConfig: Option[ParameterTool] = None
) extends Caching
          with Logging {

  // If memcachedConfig is defined, we set up memcached instead of in-memory.
  memcachedConfig.map(useMemcached)

  type VertexT = AsInetPathEntry
  type EdgeT = DefaultWeightedEdge
  type GraphT = DefaultDirectedWeightedGraph[VertexT, EdgeT]

  private var currentGraph: Option[GraphT] = None

  def graph: Option[GraphT] = currentGraph

  /** Gets every TracerouteMeta entry from the database. Uses caching.
    */
  private def getAllMeta: Iterable[TracerouteMeta] = {
    getWithCache(
      "AmpletGraph.AllMeta",
      ttl,
      {
        logger.info("Getting all TracerouteMeta from PostgreSQL...")
        postgres.getAllTracerouteMeta
      }
    ).getOrElse(Seq())
  }

  /** Gets every Traceroute measurement that is part of a stream corresponding
    * to any of the members of `metas`. The time range being queried can be
    * restricted by the `start` and `end` parameters. Uses caching.
    */
  private def getTracerouteMeasurements(
    metas: Iterable[TracerouteMeta],
    start: Instant = Instant.EPOCH,
    end  : Instant = Instant.now()
  ): Map[TracerouteMeta, Iterable[Traceroute]] = {
    getWithCache(
      s"AmpletGraph.Measurements.$start.$end",
      ttl,
      {
        logger.info(s"Getting Traceroute measurements from ${metas.size} streams...")
        Some(metas.map { m =>
          (
            m,
            postgres
              .getTracerouteData(m.stream, start, end)
              .getOrElse(Seq())
          )
        }.toMap)
      }
    ).get
  }

  /** Gets the inet and (optionally) as paths corresponding to a Traceroute
    * measurement, and merges them into a single AsInetPath. Uses caching.
    */
  private def getAsInetPath(
    trace: Traceroute,
    meta: TracerouteMeta,
    distinguishMissingInetAddresses: Boolean,
    compressMissingInetChains: Boolean
  ): Option[AsInetPath] = {
    val path: Option[TraceroutePath] = getWithCache(
      s"AmpletGraph.Path.${trace.stream}.${trace.path_id}",
      ttl,
      postgres.getTraceroutePath(trace)
    )
    val asPath: Option[TracerouteAsPath] = getWithCache(
      s"AmpletGraph.AsPath.${trace.stream}.${trace.aspath_id}",
      ttl,
      postgres.getTracerouteAsPath(trace)
    )

    path.map(p => AsInetPath(
      p.path,
      asPath.map(_.aspath),
      meta,
      distinguishMissingInetAddresses,
      compressMissingInetChains
    ))
  }

  /** Adds the vertices from an AsInetPath to the provided graph.
    */
  private def addVertices(
    graph                      : GraphT,
    path                       : AsInetPath,
    pruneMissingInetAddresses  : Boolean
  ): Unit = {
    path.foreach { hop =>
      if (!pruneMissingInetAddresses || hop.address.isDefined) {
        graph.addVertex(hop)
      }
    }
  }

  /** Adds the edges between vertices in an AsInetPath to the provided graph.
    */
  private def addEdges(
    graph                                       : GraphT,
    path                                        : AsInetPath,
    pruneMissingInetAddresses                   : Boolean
  ): Unit = {
    path.sliding(2).foreach { pairs =>
      if (
        !pruneMissingInetAddresses ||
          (pairs.head.address.isDefined && pairs.last.address.isDefined)
      ) {
        graph.addEdge(pairs.head, pairs.last)
      }
    }
  }

  /** Returns a list of vertices detected to be duplicates. The first entry in
    * the tuple is the one with less information, and should be replaced by the
    * second entry in the tuple.
    *
    * Two vertices are considered duplicates if they share the same InetAddress,
    * and one of them has the AS number category `Missing`.
    * AMP allows Traceroute tests without AS lookups, so it's possible to have
    * multiple entries for the same address, where one has no AS information. If
    * two vertices have the same InetAddress, but they have distinct,
    * non-Missing AS information, they are not considered duplicates.
    */
  private def findDuplicateVertices(graph: GraphT): Iterable[(AsInetPathEntry, AsInetPathEntry)] = {
    graph
      .vertexSet
      .asScala
      .toList
      .flatMap { vertex =>
        vertex.as.category match {
          case AsNumberCategory.Missing => Seq()
          case _ =>
            graph.vertexSet.asScala.filter { n =>
              n.as.category == AsNumberCategory.Missing && n.address == vertex.address
            }.map((_, vertex))
        }
      }
  }

  /** Replaces a vertex in the provided graph with another. `oldV` must already
    * be present in the graph. `newV` may be present in the graph, but this is
    * not a requirement. Any edges attached to `oldV` are re-attached to `newV`.
    */
  private def replaceVertex(graph: GraphT, oldV: AsInetPathEntry, newV: AsInetPathEntry): Unit = {
    val inEdges = graph.incomingEdgesOf(oldV).asScala.toList
    val outEdges = graph.outgoingEdgesOf(oldV).asScala.toList

    graph.removeVertex(oldV)
    graph.addVertex(newV)

    inEdges.foreach(edge => graph.addEdge(graph.getEdgeSource(edge), newV, edge))
    outEdges.foreach(edge => graph.addEdge(newV, graph.getEdgeTarget(edge), edge))
  }

  /** Rebuilds the graph. This operation has the potential to be expensive. Note
    * that the functions which query from the database use caching, so if the
    * ttl is not expired, the database will not be re-queried. This is useful
    * if using external caching, since the rebuild will query the cache server
    * instead of the database.
    */
  def rebuildGraph(
    start: Instant = Instant.EPOCH,
    end  : Instant = Instant.now(),
    pruneMissingInetAddresses: Boolean = true,
    distinguishMissingInetAddresses: Boolean = true,
    compressMissingInetChains: Boolean = false,
    pruneNonAmpletToAmpletHops: Boolean = true
  ): Unit = {
    val newGraph = new DefaultDirectedWeightedGraph[AsInetPathEntry, DefaultWeightedEdge](classOf[DefaultWeightedEdge])

    val measurements = getTracerouteMeasurements(
      getAllMeta,
      start, end
    )
    logger.info(s"Getting paths for ${measurements.flatMap(_._2).size} measurements...")
    measurements
      .foreach { case (meta, traceroutes) =>
        traceroutes
          .flatMap { traceroute =>
            getAsInetPath(traceroute, meta, distinguishMissingInetAddresses, compressMissingInetChains)
          }
          .foreach { path =>
            addVertices(newGraph, path, pruneMissingInetAddresses)
            addEdges(newGraph, path, pruneMissingInetAddresses)
          }
      }

    logger.info(s"Merging duplicate graph vertices...")
    logger.debug(s"Started with ${newGraph.vertexSet.size} vertices")
    findDuplicateVertices(newGraph)
      .foreach(pair => replaceVertex(newGraph, pair._1, pair._2))
    logger.debug(s"Ended with ${newGraph.vertexSet.size} vertices")

    if (pruneNonAmpletToAmpletHops) {
      // TODO
    }

    currentGraph = Some(newGraph)
  }

  /** Invalidates all caches, causing the next call to `rebuildGraph` to query
    * the database instead of the cache.
    */
  def invalidateCaches(): Unit = {
    invalidateAll()
  }
}

object AmpletGraphBuilder {
  def main(args: Array[String]): Unit = {
    val builder = new AmpletGraphBuilder(
      new PostgresConnection(
        "localhost",
        5432,
        "nntsc",
        "cuz",
        "",
        0
      ),
      ttl = None
    )

    builder.rebuildGraph()
    AmpletGraphDotExporter.exportGraph(builder.graph.get, new File("out/traceroute.dot"))
  }
}
