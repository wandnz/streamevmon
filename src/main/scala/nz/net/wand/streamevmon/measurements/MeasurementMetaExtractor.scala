package nz.net.wand.streamevmon.measurements

import nz.net.wand.streamevmon.connectors.postgres.PostgresConnection
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.traits.{Measurement, PostgresMeasurementMeta}

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

/** Outputs PostgresMeasurementMeta objects for each measurement received. Note
  * that only one Meta object will be output for each stream. if several
  * measurements that are part of the same stream are received, a corresponding
  * Meta object will only be output alongside the first entry.
  *
  * Received measurements are output unchanged through the main output. Meta
  * objects are output through a side output, which can be accessed using the
  * `outputTag` field.
  *
  * This ProcessFunction constructs a
  * [[nz.net.wand.streamevmon.connectors.postgres.PostgresConnection PostgresConnection]],
  * which uses Caching.
  *
  * @tparam MeasT The type of measurements which will be received.
  * @tparam MetaT The corresponding type of MeasurementMeta which should be
  *               output. It is the user's responsibility to ensure that any
  *               Meta obtained via the
  *               [[nz.net.wand.streamevmon.connectors.postgres.PostgresConnection.getMeta PostgresConnection.getMeta]]
  *               function are of a type which can be cast to MetaT.
  */
class MeasurementMetaExtractor[MeasT <: Measurement, MetaT <: PostgresMeasurementMeta : TypeInformation]
  extends ProcessFunction[MeasT, MeasT]
          with CheckpointedFunction
          with HasFlinkConfig
          with Logging {

  val configKeyGroup: String = "metaextractor"
  val flinkName: String = "MeasurementMeta Extractor"
  val flinkUid: String = "measurement-meta-extractor"

  @transient var pgCon: PostgresConnection = _

  override def open(parameters: Configuration): Unit = {
    val globalParams = configWithOverride(getRuntimeContext)
    pgCon = PostgresConnection(globalParams)
  }

  val outputTag = new OutputTag[MetaT]("all-measurement-meta")

  val seenMetas: mutable.Map[String, MetaT] = mutable.Map[String, MetaT]()

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

  // == CheckpointedFunction implementation ==
  // Instead of storing the entire Map in the checkpoint, we just store the
  // combined list of values for every map entry. Since each value is unique
  // and the key can be recovered from the entries, we save a bit of storage
  // complexity.

  private var checkpointState: ListState[MetaT] = _

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    checkpointState.clear()
    checkpointState.addAll(seenMetas.values.toSeq.asJava)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    val metaTTypeInfo = createTypeInformation[MetaT]
    checkpointState = context
      .getOperatorStateStore
      .getUnionListState(new ListStateDescriptor(s"measurement-meta-${metaTTypeInfo.getTypeClass.getCanonicalName}", metaTTypeInfo.getTypeClass))

    if (context.isRestored) {
      checkpointState.get.forEach { entry => seenMetas.put(entry.stream.toString, entry) }
    }
  }
}
