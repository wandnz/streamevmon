package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.connectors.postgres.PostgresConnection
import nz.net.wand.streamevmon.measurements.{Measurement, MeasurementMeta}
import nz.net.wand.streamevmon.Logging

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.collection.JavaConverters._

class MeasurementMetaExtractor[T <: Measurement]
  extends ProcessFunction[T, T]
          with CheckpointedFunction
          with HasFlinkConfig
          with Logging {

  val configKeyGroup: String = "metaextractor"
  val flinkName: String = "MeasurementMeta Extractor"
  val flinkUid: String = "measurement-meta-extractor"

  @transient private var pgCon: PostgresConnection = _

  override def open(parameters: Configuration): Unit = {
    val globalParams = configWithOverride(getRuntimeContext)
    pgCon = PostgresConnection(globalParams)
  }

  val outputTag = new OutputTag[MeasurementMeta]("all-measurement-meta")

  val seenMetas: mutable.Map[String, MeasurementMeta] = mutable.Map[String, MeasurementMeta]()

  var firstMeasurementTime: Long = 0L

  override def processElement(
    value: T,
    ctx  : ProcessFunction[T, T]#Context,
    out  : Collector[T]
  ): Unit = {
    if (!seenMetas.contains(value.stream)) {
      pgCon.getMeta(value) match {
        case Some(meta) =>
          seenMetas(value.stream) = meta
          ctx.output(outputTag, meta)
        case None =>
      }
    }

    out.collect(value)
  }

  // Not sure why we can't get a MapState from the OperatorStateStore, but this
  // workaround is fine.
  private var checkpointState: ListState[(String, MeasurementMeta)] = _

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    checkpointState.clear()
    checkpointState.addAll(seenMetas.toSeq.asJava)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    checkpointState = context
      .getOperatorStateStore
      .getListState(new ListStateDescriptor[(String, MeasurementMeta)]("measurement-meta", classOf[(String, MeasurementMeta)]))

    if (context.isRestored) {
      checkpointState.get.forEach { entry => seenMetas.put(entry._1, entry._2) }
    }
  }
}
