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
object StreamConsumer {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val sourceFunction = new RichMeasurementSourceFunction
    val processFunction = new SimpleThresholdProcessFunction[RichMeasurement]
    val sinkFunction = new InfluxSinkFunction
    val windowSize = 1

    val measurementStream = env.addSource(sourceFunction)
    val measurementWindows =
      measurementStream.windowAll(TumblingEventTimeWindows.of(Time.seconds(windowSize)))
    val eventStream = measurementWindows.process(processFunction)
    eventStream.addSink(sinkFunction)

    measurementStream.print("Measurements")
    eventStream.print("Events")

    env.execute()

    sys.exit()
  }
}
