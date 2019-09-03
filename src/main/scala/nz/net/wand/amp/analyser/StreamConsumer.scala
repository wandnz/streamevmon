package nz.net.wand.amp.analyser

import nz.net.wand.amp.analyser.flink._
import nz.net.wand.amp.analyser.measurements.RichMeasurement

import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time

/** Default entrypoint.
  *
  * Mainly used to create Flink pipelines during development, and should be
  * expected to change often.
  */
object StreamConsumer extends Logging {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    env.enableCheckpointing(10000, CheckpointingMode.EXACTLY_ONCE)

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    val sourceFunction = new RichMeasurementSubscriptionSourceFunction
    val processFunction = new SimpleThresholdProcessFunction[RichMeasurement]
    val sinkFunction = new InfluxSinkFunction
    val windowSize = 1

    val measurementStream = env.addSource(sourceFunction)
      .name("InfluxDB Subscription Rich Measurement Source Function")

    val streamWithWatermarks = measurementStream
      .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[RichMeasurement](Time.seconds(windowSize)) {
        override def extractTimestamp(element: RichMeasurement): Long = element.time.toEpochMilli
      })

    val measurementWindows =
      streamWithWatermarks.windowAll(TumblingEventTimeWindows.of(Time.seconds(windowSize)))

    val eventStream = measurementWindows.process(processFunction)
      .name("Simple Rich ICMP Threshold Filter")

    eventStream.addSink(sinkFunction)
      .name("InfluxDB Measurement Sink Function")

    measurementStream.print("Measurements")
    eventStream.print("Events")

    env.execute("InfluxDB to Simple ICMP Threshold to InfluxDB")
  }
}
