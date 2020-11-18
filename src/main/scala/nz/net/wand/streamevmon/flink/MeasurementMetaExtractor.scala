package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.connectors.postgres.PostgresConnection
import nz.net.wand.streamevmon.measurements.{Measurement, PostgresMeasurementMeta}
import nz.net.wand.streamevmon.Logging

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo._
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.collection.JavaConverters._

class MeasurementMetaExtractor[MeasT <: Measurement, MetaT <: PostgresMeasurementMeta : TypeInformation]
  extends ProcessFunction[MeasT, MeasT]
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

  val outputTag = new OutputTag[MetaT]("all-measurement-meta")

  val seenMetas: mutable.Map[String, MetaT] = mutable.Map[String, MetaT]()

  var firstMeasurementTime: Long = 0L

  override def processElement(
    value: MeasT,
    ctx  : ProcessFunction[MeasT, MeasT]#Context,
    out  : Collector[MeasT]
  ): Unit = {
    if (!seenMetas.contains(value.stream)) {
      pgCon.getMeta(value) match {
        case Some(meta) =>
          val metaAsMetaT = meta.asInstanceOf[MetaT]
          seenMetas(value.stream) = metaAsMetaT
          ctx.output(outputTag, metaAsMetaT)
        case None =>
      }
    }

    out.collect(value)
  }

  // Not sure why we can't get a MapState from the OperatorStateStore, but this
  // workaround is fine.
  private var checkpointState: ListState[(String, MetaT)] = _

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    checkpointState.clear()
    checkpointState.addAll(seenMetas.toSeq.asJava)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    checkpointState = context
      .getOperatorStateStore
      .getListState(new ListStateDescriptor[(String, MetaT)](s"measurement-meta-${createTypeInformation[MetaT].getTypeClass.getCanonicalName}", classOf[(String, MetaT)]))

    if (context.isRestored) {
      checkpointState.get.forEach { entry => seenMetas.put(entry._1, entry._2) }
    }
  }
}
