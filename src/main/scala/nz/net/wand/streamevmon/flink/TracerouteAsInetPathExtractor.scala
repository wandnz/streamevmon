package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.{Caching, Logging}
import nz.net.wand.streamevmon.connectors.postgres.{AsInetPath, PostgresConnection}
import nz.net.wand.streamevmon.measurements.amp._

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

class TracerouteAsInetPathExtractor(
  ttl: Option[FiniteDuration] = None
)
  extends CoProcessFunction[Traceroute, TracerouteMeta, AsInetPath]
          with HasFlinkConfig
          with CheckpointedFunction
          with Caching
          with Logging {

  override val flinkName: String = "Traceroute AsInetPath Extractor"
  override val flinkUid: String = "asinetpath-extractor"
  override val configKeyGroup: String = "postgres"

  @transient private var pgCon: PostgresConnection = _

  protected val knownMetas: mutable.Map[String, TracerouteMeta] = mutable.Map()

  protected val unprocessedMeasurements: mutable.Map[String, List[Traceroute]] = mutable.Map()

  override def open(parameters: Configuration): Unit = {
    val params = configWithOverride(getRuntimeContext)
    pgCon = PostgresConnection(params)
  }

  protected def getAsInetPath(
    trace: Traceroute,
    meta : TracerouteMeta
  ): Option[AsInetPath] = {
    val path: Option[TraceroutePath] = getWithCache(
      s"AmpletGraph.Path.${trace.stream}.${trace.path_id}",
      ttl,
      pgCon.getTraceroutePath(trace)
    )
    val asPath: Option[TracerouteAsPath] = getWithCache(
      s"AmpletGraph.AsPath.${trace.stream}.${trace.aspath_id}",
      ttl,
      pgCon.getTracerouteAsPath(trace)
    )

    if (path.isEmpty || asPath.isEmpty) {
      logger.warn(s"Failed to get TraceroutePath or TracerouteAsPath! Values: $path, $asPath")
    }

    path.map(p => AsInetPath(
      p.path,
      asPath.map(_.aspath),
      trace,
      meta
    ))
  }

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
      case Some(meta) => getAsInetPath(value, meta).foreach(out.collect)
    }
  }

  override def processElement2(
    value: TracerouteMeta,
    ctx  : CoProcessFunction[Traceroute, TracerouteMeta, AsInetPath]#Context,
    out  : Collector[AsInetPath]
  ): Unit = {
    val stream = value.stream.toString
    knownMetas.put(stream, value)
    unprocessedMeasurements.get(stream).foreach(_.foreach { meas =>
      processElement1(meas, ctx, out)
    })
    unprocessedMeasurements.put(stream, List())
  }

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
