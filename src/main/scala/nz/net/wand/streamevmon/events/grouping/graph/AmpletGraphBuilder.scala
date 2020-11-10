package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.{Caching, Logging}
import nz.net.wand.streamevmon.connectors.postgres._
import nz.net.wand.streamevmon.measurements.amp._

import java.io.{File, FileOutputStream, ObjectOutputStream}
import java.time.Instant

import org.apache.flink.api.java.utils.ParameterTool
import org.jgrapht.graph.{DefaultDirectedWeightedGraph, DefaultWeightedEdge, GraphWalk}

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

object AmpletGraphBuilder {
  def main(args: Array[String]): Unit = {
    val pgCon = new PostgresConnection(
      "localhost",
      5432,
      "nntsc",
      "cuz",
      "",
      0
    )

    val builder = new AmpletGraphBuilder(pgCon, ttl = None)

    val startTime = System.currentTimeMillis()

    val paths = builder.getAsInetPathsFromDatabase(
      pruneNonAmpletToAmpletHops = true
    )

    val endTime = System.currentTimeMillis()

    pgCon.closeSession()

    println(s"Getting AsInetPaths took ${endTime - startTime}ms")
    println(s"Used ${pgCon.transactionCounter} transactions")
    println(s"Approx ${(endTime - startTime) / pgCon.transactionCounter}ms/transaction")

    val graph = builder.buildGraphFromPaths(paths)

    val endTime2 = System.currentTimeMillis()
    println(s"Building graph took ${endTime2 - endTime}ms")

    AmpletGraphDotExporter.exportGraph(graph, new File("out/traceroute_cauldron.dot"))
    val oos = new ObjectOutputStream(new FileOutputStream("out/traceroute_cauldron.spkl"))
    oos.writeObject(graph)
    val oos2 = new ObjectOutputStream(new FileOutputStream("out/traceroute_paths_cauldron.spkl"))
    oos2.writeObject(paths)
  }
}

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

  /** Invalidates all caches. */
  def invalidateCaches(): Unit = {
    invalidateAll()
  }

  type VertexT = Host
  type EdgeT = DefaultWeightedEdge
  type GraphT = DefaultDirectedWeightedGraph[VertexT, EdgeT]

  /** Gets every TracerouteMeta entry from the database. Uses caching. */
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
    meta: TracerouteMeta
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
      trace,
      meta
    ))
  }

  /** Gets all the AsInetPaths that represent the measurements in the database
    * that occurred between `start` and `end`.
    *
    * @param pruneNonAmpletToAmpletHops If true, only measurements which are a
    *                                   traceroute between two amplets are
    *                                   considered. If false, the time taken and
    *                                   number of database queries will be
    *                                   considerably larger (10x has been
    *                                   observed).
    */
  def getAsInetPathsFromDatabase(
    start: Instant = Instant.EPOCH,
    end              : Instant = Instant.now(),
    pruneNonAmpletToAmpletHops: Boolean = true
  ): Iterable[AsInetPath] = {
    val metas = getAllMeta

    val usedMetas = if (pruneNonAmpletToAmpletHops) {
      val knownAmplets = metas.map(_.source).toSet
      val result = metas.filter(m => knownAmplets.contains(m.destination))
      logger.debug(s"Only using ${result.size} amplet-to-amplet streams of ${metas.size} total")
      result
    }
    else {
      metas
    }

    val measurements = getTracerouteMeasurements(
      usedMetas,
      start, end
    )
    logger.info(s"Getting paths for ${measurements.flatMap(_._2).size} measurements...")
    measurements
      .flatMap { case (meta, traceroutes) =>
        traceroutes
          .flatMap { traceroute =>
            getAsInetPath(traceroute, meta)
          }
      }
  }

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

  /** Builds a graph describing the connectivity of known hosts from a
    * collection of AsInetPaths.
    *
    * This function is expensive. It takes time to construct the graph, and the
    * runtime grows faster than linearly with the number of paths.
    *
    * Each Host has a `knownPaths` field, which contains a fully populated set
    * of GraphWalks. These mirror the paths followed by the input AsInetPaths.
    */
  def buildGraphFromPaths(paths: Iterable[AsInetPath]): GraphT = {
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

    // This block does double duty setting up structures that need knowledge
    // of all hosts. For each known host, we first add it to the graph, and then
    // create a map entry whose value is an empty Set to store GraphWalks in.
    val knownPathLookupTable: Map[Host, mutable.Set[GraphWalk[VertexT, EdgeT]]] =
    mergedHostLookupTable.map { case (_, host) =>
      graph.addVertex(host)
      (host, mutable.Set[GraphWalk[VertexT, EdgeT]]())
    }

    // Add all the paths to the graph as edges.
    pathsAndHosts.foreach { case (path, hosts) =>
      path
        .zip(hosts)
        .sliding(2)
        .foreach { elems =>
          val source = elems.head
          val dest = elems.tail

          // Paths that only have one element will give an empty sequence for
          // dest, hence using foreach will skip creating an edge in that case.
          dest.foreach { v =>
            graph.addEdge(
              mergedHostLookupTable(source._2.uid),
              mergedHostLookupTable(v._2.uid)
            )
          }
        }

      // Create a walk for this path. We can't construct these in advance since
      // construction depends on the edges existing in the graph. Unfortunately
      // this means we waste some time constructing duplicate walks, since they
      // get stored in a Set.
      val walk = new GraphWalk[VertexT, EdgeT](
        graph,
        hosts.map(h => mergedHostLookupTable(h.uid)).toList.asJava,
        0.0
      )

      // Update the set of known GraphWalks for each host that is part of it.
      hosts.foreach(h => knownPathLookupTable(h) += walk)
    }

    // Attach the walks to their hosts
    graph.vertexSet.forEach { v =>
      v.knownPaths ++= knownPathLookupTable(v)
    }

    graph
  }

  def buildGraphFromDatabase(
    start: Instant = Instant.EPOCH,
    end  : Instant = Instant.now(),
    pruneNonAmpletToAmpletHops: Boolean = true
  ): GraphT = {
    buildGraphFromPaths(getAsInetPathsFromDatabase(
      start, end, pruneNonAmpletToAmpletHops
    ))
  }
}
