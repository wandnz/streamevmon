package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.{Caching, Logging}
import nz.net.wand.streamevmon.connectors.postgres.PostgresConnection
import nz.net.wand.streamevmon.connectors.postgres.schema.AsInetPath
import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.amp._

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.collection.JavaConverters._

/** Receives a stream of Traceroute measurements, and outputs one AsInetPath
  * for each input measurement (assuming nothing goes wrong in the fetching
  * process).
  *
  * The second input is a stream of TracerouteMeta entries. It is expected that
  * each Traceroute input will have a corresponding TracerouteMeta that belongs
  * to the same stream, but it is not required that the TracerouteMeta arrives
  * first. Any Traceroute measurements that were received before the
  * corresponding TracerouteMeta are buffered and processed when the Meta
  * is received.
  *
  * If a TracerouteMeta is received that is part of a stream we already have an
  * entry for, it is simply used from then on, overwriting the previous entry.
  *
  * This CoProcessFunction constructs a
  * [[nz.net.wand.streamevmon.connectors.postgres.PostgresConnection PostgresConnection]],
  * which uses Caching.
  */
class TracerouteAsInetPathExtractor
  extends CoProcessFunction[Traceroute, TracerouteMeta, AsInetPath]
          with HasFlinkConfig
          with CheckpointedFunction
          with Caching
          with Logging {

  override val flinkName: String = "Traceroute AsInetPath Extractor"
  override val flinkUid: String = "asinetpath-extractor"
  override val configKeyGroup: String = "postgres"

  @transient var pgCon: PostgresConnection = _

  protected val knownMetas: mutable.Map[String, TracerouteMeta] = mutable.Map()

  protected val unprocessedMeasurements: mutable.Map[String, List[Traceroute]] = mutable.Map()

  override def open(parameters: Configuration): Unit = {
    val params = configWithOverride(getRuntimeContext)
    if (pgCon == null) {
      pgCon = PostgresConnection(params)
    }
  }

  /** Converts a Traceroute and its corresponding TracerouteMeta into an
    * AsInetPath. Performs database communication via `pgCon`.
    */
  protected def getAsInetPath(
    trace: Traceroute,
    meta : TracerouteMeta
  ): Option[AsInetPath] = {
    val path: Option[TraceroutePath] = pgCon.getTraceroutePath(trace)
    val asPath: Option[TracerouteAsPath] = trace.aspath_id.flatMap(_ => pgCon.getTracerouteAsPath(trace))

    // If we couldn't find one of the paths we should have, just log a warning
    // and continue as usual. If the path is empty, no AsInetPath is returned.
    if (path.isEmpty || (trace.aspath_id.isDefined && asPath.isEmpty)) {
      logger.warn(s"Failed to get TraceroutePath or TracerouteAsPath! Values: $path, $asPath")
    }

    path.map(p => AsInetPath(
      p.path,
      asPath.map(_.aspath),
      trace,
      meta
    ))
  }

  /** Outputs an AsInetPath for each Traceroute measurement received. If a
    * corresponding TracerouteMeta has not yet been received via
    * `processElement2`, we store the Traceroute for later processing.
    */
  override def processElement1(
    value: Traceroute,
    ctx: CoProcessFunction[Traceroute, TracerouteMeta, AsInetPath]#Context,
    out  : Collector[AsInetPath]
  ): Unit = {
    knownMetas.get(value.stream) match {
      // If we don't have a meta for this yet, we can't construct an output.
      // Write it down so we can process it when the meta comes in.
      case None => unprocessedMeasurements.put(
        value.stream,
        // Append to the existing list if it's there, or make a new one.
        unprocessedMeasurements.getOrElse(value.stream, List()) :+ value
      )
      // If we do have a meta we can go ahead and make our AsInetPath.
      case Some(meta) =>
        val p = getAsInetPath(value, meta)
        p.foreach(out.collect)
    }
  }

  /** When a TracerouteMeta is received, it is recorded to be used with future
    * Traceroute measurements that are received. If any unprocessed Traceroute
    * measurements corresponding to the new Meta were previously received, we
    * process them now.
    *
    * If a TracerouteMeta is received for a stream that we previously had an
    * entry for, it is simply overwritten with the new one, and the function
    * will begin to use it from then on.
    */
  override def processElement2(
    value: TracerouteMeta,
    ctx: CoProcessFunction[Traceroute, TracerouteMeta, AsInetPath]#Context,
    out  : Collector[AsInetPath]
  ): Unit = {
    val stream = value.stream.toString
    knownMetas.put(stream, value)
    unprocessedMeasurements.get(stream).foreach(_.foreach { meas =>
      processElement1(meas, ctx, out)
    })
    unprocessedMeasurements.put(stream, List())
  }

  // == CheckpointedFunction implementation ==
  // Instead of storing the entire Map in the checkpoint, we just store the
  // combined list of values for every map entry. Since each value is unique
  // and the key can be recovered from the entries, we save a bit of storage
  // complexity.

  private var knownMetasState: ListState[TracerouteMeta] = _
  private var unprocessedMeasurementsState: ListState[Traceroute] = _

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    knownMetasState.clear()
    unprocessedMeasurementsState.clear()

    knownMetasState.addAll(knownMetas.values.toSeq.asJava)
    unprocessedMeasurementsState.addAll(unprocessedMeasurements.flatMap(_._2).toSeq.asJava)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    knownMetasState = context
      .getOperatorStateStore
      .getUnionListState(new ListStateDescriptor[TracerouteMeta](
        "asinetpathextractor-knownMetas", classOf[TracerouteMeta]
      ))

    unprocessedMeasurementsState = context
      .getOperatorStateStore
      .getUnionListState(new ListStateDescriptor[Traceroute](
        "asinetpathextractor-unprocessedMeasurements", classOf[Traceroute]
      ))

    if (context.isRestored) {
      knownMetasState.get.forEach(entry => knownMetas.put(entry.stream.toString, entry))
      unprocessedMeasurementsState.get.forEach { entry =>
        unprocessedMeasurements.getOrElse(entry.stream, List()) :+ entry
      }
    }
  }
}
