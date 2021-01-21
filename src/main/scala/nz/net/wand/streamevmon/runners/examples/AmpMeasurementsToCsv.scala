package nz.net.wand.streamevmon.runners.examples

import nz.net.wand.streamevmon.{Configuration, Logging}
import nz.net.wand.streamevmon.flink.sources.LatencyTSAmpFileInputFormat
import nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP

import java.io.OutputStream
import java.time.{Duration, Instant}

import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.core.fs.Path
import org.apache.flink.core.io.SimpleVersionedSerializer
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.sink.filesystem.{BucketAssigner, StreamingFileSink}
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.SimpleVersionedStringSerializer
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy
import org.apache.flink.streaming.api.functions.source.{FileProcessingMode, SourceFunction}
import org.apache.flink.streaming.api.scala._

/** Shows saving measurements that implement
  * [[nz.net.wand.streamevmon.measurements.traits.CsvOutputable CsvOutputable]] to a
  * file using the StreamingFileSink.
  *
  * This example is more convoluted than it needs to be in order to get around
  * restrictions of the file sink in regards to checkpointing and finite
  * streams. If a similar layout was used for an infinite stream, it would be
  * much shorter.
  *
  * The StreamingFileSink writes to temporary files, and will solidify them
  * when checkpoints occur if certain conditions are fulfilled. In this example,
  * we use a processing time condition that will only roll a file every 5
  * seconds. This means that all our example files can be put into one output
  * file.
  */
object AmpMeasurementsToCsv {

  // We keep track of the last time a record was parsed so that we can exit
  // at the right time. We must exit un-gracefully, since checkpoints can't be
  // taken if any functions are FINISHED. We leave the file source running
  // and looking for changes (which won't happen).
  var lastRecordTime: Instant = Instant.MAX

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val config = Configuration.get(args)
    env.getConfig.setGlobalJobParameters(config)

    env.disableOperatorChaining()

    // We must set up checkpointing so that the sink will solidify its output files.
    env.enableCheckpointing(Duration.ofSeconds(1).toMillis, CheckpointingMode.EXACTLY_ONCE)
    env.setRestartStrategy(RestartStrategies.noRestart())

    val measurements = env
      .readFile(
        // This is a normal inputformat, except that it updates our lastRecordTime.
        new LatencyTSAmpFileInputFormat with Logging {
          override def readRecord(reuse: LatencyTSAmpICMP, bytes: Array[Byte], offset: Int, numBytes: Int): LatencyTSAmpICMP = {
            AmpMeasurementsToCsv.lastRecordTime = Instant.now()
            super.readRecord(reuse, bytes, offset, numBytes)
          }
        },
        "data/latency-ts-i/ampicmp/series",
        // We process continually so that the sourcefunction keeps running after
        // it's done. We don't expect any updated files.
        FileProcessingMode.PROCESS_CONTINUOUSLY,
        Duration.ofSeconds(1).toMillis
      )
      .setParallelism(1)
      .name("AMP measurement source")
      .uid("amp-source")

    // This constructs a file sink. It will put each stream into its own folder,
    // since we use a BucketAssigner which sets bucket ID based on stream ID.
    val sink: StreamingFileSink[Seq[String]] =
    StreamingFileSink.forRowFormat(new Path("out/ampOutput"),
      // This just outputs our Seq in CSV format. I couldn't get any of the
      // built in ones to work nicely with variable numbers of entries.
      (element: Seq[String], stream: OutputStream) => {
        if (element.nonEmpty) {
          stream.write(element.head.getBytes)
          element.drop(1).foreach { e =>
            stream.write(',')
            stream.write(e.getBytes)
          }
          stream.write('\n')
        }
      })
      // The bucket ID is just the stream ID, so we keep all the data of the
      // same type together.
      .withBucketAssigner(
        new BucketAssigner[Seq[String], String] {
          override def getBucketId(element: Seq[String], context: BucketAssigner.Context): String = element.head

          override def getSerializer: SimpleVersionedSerializer[String] = SimpleVersionedStringSerializer.INSTANCE
        }
      )
      // We only roll buckets every 5 seconds. This gives us plenty of time
      // to write an entire file, but still gives us a bit of leeway to
      // finalise it before the program exits.
      .withRollingPolicy(
        DefaultRollingPolicy.builder()
          .withRolloverInterval(Duration.ofSeconds(5).toMillis)
          .build()
      )
      // We check for bucket completeness often. Could be less often and would
      // probably improve performance, but this is fine.
      .withBucketCheckInterval(Duration.ofMillis(50).toMillis)
      .build()

    val asCsv = measurements
      .map(_.toCsvFormat)
      .name("Convert to CSV format")
      .uid("map-toCsvFormat")

    asCsv
      .addSink(sink)
      .setParallelism(1)
      .name("Write to file")
      .uid("sink-to-file")

    // This is a dummy source that never outputs any records. It counts how
    // many checkpoints have happened, and checks how long it's been since the
    // last record was parsed. If this gets too long, we throw an exception to
    // end the program. There's no way in Flink to exit a pipeline gracefully
    // without all the sources going to FINISHED, but that means checkpoints
    // can't happen anymore, which breaks the StreamingFileSink's finalisation
    // policy.
    env
      .addSource(new SourceFunction[String] with CheckpointedFunction with Logging {
        var checkpointCount = 0

        override def run(ctx: SourceFunction.SourceContext[String]): Unit = {
          var timeSinceLastRecord = Duration.between(AmpMeasurementsToCsv.lastRecordTime, Instant.now())
          while (timeSinceLastRecord.compareTo(Duration.ofSeconds(5)) < 0) {
            logger.error(s"Last record read was $lastRecordTime ($timeSinceLastRecord ago)")
            logger.error(s"Checkpoint count is $checkpointCount")
            Thread.sleep(2500)
            timeSinceLastRecord = Duration.between(AmpMeasurementsToCsv.lastRecordTime, Instant.now())
          }
          logger.error(s"Done! Checkpoint count is $checkpointCount")
          class FinishedJobException extends RuntimeException
          throw new FinishedJobException
        }

        override def cancel(): Unit = {}

        override def snapshotState(context: FunctionSnapshotContext): Unit = checkpointCount += 1

        override def initializeState(context: FunctionInitializationContext): Unit = {}
      })
      .print()

    env.execute()
  }
}


