package nz.net.wand.streamevmon.events

import nz.net.wand.streamevmon.{Caching, Logging}
import nz.net.wand.streamevmon.connectors.postgres._
import nz.net.wand.streamevmon.measurements.amp._

import java.io.{File, FileOutputStream, ObjectOutputStream}
import java.time.Instant

import org.apache.flink.api.java.utils.ParameterTool
import org.jgrapht.graph.{DefaultDirectedWeightedGraph, DefaultWeightedEdge}

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

    println(s"Getting AsInetPaths took ${endTime - startTime}ms")
    println(s"Used ${pgCon.transactionCounter} transactions")
    println(s"Approx ${(endTime - startTime) / pgCon.transactionCounter}ms/transaction")

    pgCon.closeSession()
    return

    val graph = AmpletGraphBuilder2.buildGraph(paths)

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

  type VertexT = AsInetPathEntry
  type EdgeT = DefaultWeightedEdge
  type GraphT = DefaultDirectedWeightedGraph[VertexT, EdgeT]

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
      trace,
      meta,
      distinguishMissingInetAddresses,
      compressMissingInetChains
    ))
  }

  def getAsInetPathsFromDatabase(
    start: Instant = Instant.EPOCH,
    end: Instant = Instant.now(),
    distinguishMissingInetAddresses: Boolean = true,
    compressMissingInetChains      : Boolean = false,
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
            getAsInetPath(traceroute, meta, distinguishMissingInetAddresses, compressMissingInetChains)
          }
      }
  }

  /** Invalidates all caches. */
  def invalidateCaches(): Unit = {
    invalidateAll()
  }
}
