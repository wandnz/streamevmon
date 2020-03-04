package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.flink.AmpMeasurementSourceFunction
import nz.net.wand.streamevmon.Configuration

import java.io.OutputStream
import java.time.Duration

import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.time.Time
import org.apache.flink.core.fs.Path
import org.apache.flink.core.io.SimpleVersionedSerializer
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.functions.sink.filesystem.{BucketAssigner, StreamingFileSink}
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.SimpleVersionedStringSerializer
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy
import org.apache.flink.streaming.api.scala._

object AmpMeasurementsToCsv {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("influx.dataSource.amp.subscriptionName", "AmpMeasurementsToCsv")

    val config = Configuration.get(args)
    env.getConfig.setGlobalJobParameters(config)

    env.disableOperatorChaining()

    env.enableCheckpointing(Duration.ofSeconds(30).toMillis, CheckpointingMode.EXACTLY_ONCE)
    env.setRestartStrategy(RestartStrategies.fixedDelayRestart(Int.MaxValue, Time.seconds(1)))

    val input = new AmpMeasurementSourceFunction(fetchHistory = Duration.ofSeconds(60))

    val measurements = env
      .addSource(input)
      .setParallelism(1)
      .name("AMP measurement source")
      .uid("amp-source")

    val sink: StreamingFileSink[Iterable[String]] =
      StreamingFileSink.forRowFormat(new Path("out/ampOutput"),
        (element: Iterable[String], stream: OutputStream) => {
          if (element.nonEmpty) {
            stream.write(element.head.getBytes)
            element.drop(1).foreach { e =>
              stream.write(',')
              stream.write(e.getBytes)
            }
            stream.write('\n')
          }
        })
        .withBucketAssignerAndPolicy(
          new BucketAssigner[Iterable[String], String] {
            override def getBucketId(element: Iterable[String], context: BucketAssigner.Context): String = element.head

            override def getSerializer: SimpleVersionedSerializer[String] = SimpleVersionedStringSerializer.INSTANCE
          },
          DefaultRollingPolicy.create()
            .withRolloverInterval(Duration.ofDays(14).toMillis)
            .withInactivityInterval(Duration.ofDays(14).toMillis)
            .build()
        )
        .build()

    measurements
      .map(_.toCsvFormat)
      .name("Convert to CSV format")
      .uid("map-toCsvFormat")
      .addSink(sink)
      .setParallelism(1)
      .name("Write to file")
      .uid("sink-to-file")

    env.execute()
  }
}
