package nz.net.wand.streamevmon.runners.unified

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.bigdata.Flow
import nz.net.wand.streamevmon.runners.unified.schema.SourceReferenceDatatype

import java.time.Duration

import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
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

    val sources = flows.sources.mapValues(_.build)

    val detectorsBySink: mutable.Map[String, mutable.Buffer[DataStream[Event]]] = mutable.Map()
    val sinks = flows.sinks.map { case (name, sink) =>
      detectorsBySink(name) = mutable.Buffer()
      (name, sink.build)
    }

    val sourcesWithStreams = sources.map { case (name, source) => (
      name,
      (
        source,
        env
          .addSource(source)
          .name(s"$name (${source.flinkName})")
          .uid(s"${source.flinkUid}-$name")
      )
    )
      // This ain't lazy, and it really needs to be. We might have to change
      // everything to lambdas. While we do that, we should make some wrapper
      // classes so that the structures generated are much more readable.
    }.map { case (name, (source, stream)) => (
      name,
      (
        source,
        stream,
        SourceReferenceDatatype.values.map {
          case v@SourceReferenceDatatype.DNS =>
            val filtered = stream.filterType[DNS]
            (v, (filtered, filtered.notLossy[DNS]))
          case v@SourceReferenceDatatype.HTTP =>
            val filtered = stream.filterType[HTTP]
            (v, (filtered, filtered.notLossy[HTTP]))
          case v@SourceReferenceDatatype.ICMP =>
            val filtered = stream.filterType[ICMP]
            (v, (filtered, filtered.notLossy[ICMP]))
          case v@SourceReferenceDatatype.TCPPing =>
            val filtered = stream.filterType[TCPPing]
            (v, (filtered, filtered.notLossy[TCPPing]))
          case v@SourceReferenceDatatype.Traceroute =>
            val filtered = stream.filterType[Traceroute]
            (v, (filtered, filtered.notLossy[Traceroute]))
          case v@SourceReferenceDatatype.Flow =>
            val filtered = stream.filterType[Flow]
            (v, (filtered, filtered.notLossy[Flow]))
        }.toMap
      )
    )
    }

    flows.detectors.foreach { case (name, detSchema) =>
      detSchema.instances.foreach { detInstance =>
        val sourcesList = detInstance.sources.map(s => (s, sourcesWithStreams(s.name)))
        // Just one input source for now
        val eventStream: DataStream[Event] = sourcesList.headOption.map {
          case (srcReference, (sourceFunction, dataStream, filteredStreams)) =>
            val detector = detInstance.build(detSchema.detType, detSchema.config)
            val stream = filteredStreams(srcReference.datatype)

            val selectedStream = if (srcReference.filterLossy) {
              stream._2
            }
            else {
              stream._1
            }

            selectedStream
              .keyAndTimeWindowStreams
              .keyed
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
