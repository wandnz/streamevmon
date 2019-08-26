package nz.net.wand.amp.analyser

import nz.net.wand.amp.analyser.flink._
import nz.net.wand.amp.analyser.measurements.RichMeasurement

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.TimeCharacteristic

/** Default entrypoint.
  *
  * Mainly used to create Flink pipelines during development, and should be
  * expected to change often.
  */
object StreamConsumer extends Logging {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    val sourceFunction = new RichMeasurementSubscriptionSourceFunction
    val processFunction = new SimpleThresholdProcessFunction[RichMeasurement]
    val sinkFunction = new InfluxSinkFunction
    val windowSize = 1

    val measurementStream = env.addSource(sourceFunction)
      .name("InfluxDB Subscription Rich Measurement Source Function")

    val measurementWindows =
      measurementStream.windowAll(TumblingEventTimeWindows.of(Time.seconds(windowSize)))

    val eventStream = measurementWindows.process(processFunction)
      .name("Simple Rich ICMP Threshold Filter")

    eventStream.addSink(sinkFunction)
      .name("InfluxDB Measurement Sink Function")

    measurementStream.print("Measurements")
    eventStream.print("Events")

    env.execute()
  }
}
