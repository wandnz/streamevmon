package nz.net.wand.streamevmon.flink.sources

import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.connectors.postgres.PostgresConnection
import nz.net.wand.streamevmon.measurements.amp.{Traceroute, TracerouteMeta}

import java.time.{Duration, Instant}

import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}

class PostgresTracerouteSourceFunction(
  fetchHistory: Duration = Duration.ZERO
)
  extends RichSourceFunction[Traceroute]
          with HasFlinkConfig
          with CheckpointedFunction
          with Logging {
  override val flinkName: String = "PostgreSQL Measurement Source"
  override val flinkUid: String = "postgres-measurement-source"
  override val configKeyGroup: String = "postgres"

  @volatile
  @transient protected var isRunning = false

  var lastMeasurementTime: Instant = Instant.now().minus(fetchHistory)

  def refreshUsedStreams(
    pgCon         : PostgresConnection,
    ampletToAmplet: Boolean = true
  ): Iterable[Int] = {
    logger.info("Refreshing traceroute stream library...")
    pgCon.getAllTracerouteMeta match {
      case Some(value) =>
        // We construct a list of potential filters and whether they're enabled.
        // Each filter corresponds to a boolean argument that was passed to
        // this function.
        Seq[(Boolean, TracerouteMeta => Boolean)](
          // Only retains streams that have a known amplet as a destination
          (ampletToAmplet, _ => true)
          // Next, we apply all the filters to the list of all the streams.
          // The order doesn't matter, but this is easier to write than .fold()
        ).foldLeft(value) {
          // If the filter was enabled by the corresponding function argument...
          case (metas, (enabled, func)) => if (enabled) {
            // apply it
            metas.filter(func)
          }
          else {
            // otherwise do nothing
            metas
          }
          // Finally, get the streams of the successful metas and assign
          // usedStreams to the result.
        }.map(_.stream)
      case None =>
        logger.info("Error getting TracerouteMeta entries.")
        None
    }
  }

  override def run(ctx: SourceFunction.SourceContext[Traceroute]): Unit = {
    val params = configWithOverride(getRuntimeContext)
    val pgCon = PostgresConnection(params, configKeyGroup)
    val refreshDelay = params.getLong(s"source.$configKeyGroup.tracerouteRefreshDelay")

    if (getRuntimeContext.getNumberOfParallelSubtasks > 1) {
      throw new IllegalStateException("Parallelism for this SourceFunction must be 1.")
    }

    isRunning = true

    while (isRunning) {
      val usedStreams = refreshUsedStreams(pgCon)
      val now = Instant.now()
      logger.info(s"Getting traceroute measurements for ${usedStreams.size} streams between $lastMeasurementTime and $now...")
      usedStreams.flatMap { stream =>
        pgCon.getTracerouteData(stream, lastMeasurementTime, now)
      }
        .flatten
        .foreach { meas =>
          if (meas.timestamp > lastMeasurementTime.getEpochSecond) {
            lastMeasurementTime = meas.time
          }
          ctx.collect(meas)
        }
      Thread.sleep(Duration.ofSeconds(refreshDelay).toMillis)
    }
  }

  override def cancel(): Unit = {
    logger.info("Stopping listener...")
    isRunning = false
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {

  }

  override def initializeState(context: FunctionInitializationContext): Unit = {

  }
}
