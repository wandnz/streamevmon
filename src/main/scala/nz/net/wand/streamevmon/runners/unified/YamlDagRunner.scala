package nz.net.wand.streamevmon.runners.unified

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.HasFlinkConfig
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.runners.unified.schema.{Lazy, SourceAndFilters}

import java.time.Duration

import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.flink.streaming.api.scala._

import scala.collection.mutable

object YamlDagRunner extends UnifiedRunnerExtensions {

  def main(args: Array[String]): Unit = {
    // == Setup flink config ==
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    config = Configuration.get(args)
    env.getConfig.setGlobalJobParameters(config)

    env.disableOperatorChaining

    env.enableCheckpointing(
      Duration.ofSeconds(config.getInt("flink.checkpointInterval")).toMillis,
      CheckpointingMode.EXACTLY_ONCE
    )

    env.setRestartStrategy(RestartStrategies.noRestart())

    // == Parse flow config key ==
    val flows = Configuration.getFlowsDag

    val detectorsBySink: mutable.Map[String, mutable.Buffer[DataStream[Event]]] = mutable.Map()
    val sinks: Map[String, SinkFunction[Event] with HasFlinkConfig] =
      flows.sinks.map {
        case (name, sink) =>
          detectorsBySink(name) = mutable.Buffer()
          (name, sink.build)
      }

    val sources: Map[String, SourceAndFilters] =
      flows.sources.map {
        case (name, sourceInstance) =>
          val lazyBuilt = new Lazy({
            val built = sourceInstance.build
            env
              .addSource(built)
              .name(s"$name (${built.flinkName})")
              .uid(s"${built.flinkUid}-$name")
          })

          (
            name,
            SourceAndFilters(lazyBuilt)
          )
      }

    flows.detectors.foreach {
      case (name, detSchema) =>
        detSchema.instances.foreach { detInstance =>
          val sourcesList = detInstance.sources.map(s => (s, sources(s.name)))
          // Just one input source for now
          val eventStream: DataStream[Event] = sourcesList.headOption
            .map {
              case (srcReference, streamWithFilters) =>
                val detector = detInstance.build(detSchema.detType, detSchema.config)

                val stream = if (srcReference.filterLossy) {
                  streamWithFilters.typedAs(srcReference.datatype).notLossyKeyedStream
                }
                else {
                  streamWithFilters.typedAs(srcReference.datatype).keyedStream
                }

                stream
                  .process(detector)
                  .name(s"$name (${detector.flinkName})")
                  .uid(s"${detector.flinkUid}-$name")
            }
            .getOrElse(
              throw new IllegalArgumentException("Detector instance must have at least one source!")
            )

          detInstance.sinks.foreach { sink =>
            detectorsBySink(sink.name).append(eventStream)
          }
        }
    }

    detectorsBySink.foreach {
      case (sinkName, dets) =>
        val union = dets.size match {
          case 0 => None
          case 1 => Some(dets.head)
          case _ => Some(dets.head.union(dets.drop(1): _*))
        }

        union.map { dets =>
          dets
            .addSink(sinks(sinkName))
            .name(s"$sinkName (${sinks(sinkName).flinkName})")
            .uid(s"${sinks(sinkName).flinkUid}-$sinkName")
        }
    }

    val breakpoint = 1

    env.execute()
  }
}
