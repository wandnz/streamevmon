package nz.net.wand.streamevmon.runners.detectors

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.loss.LossDetector
import nz.net.wand.streamevmon.flink.MeasurementKeySelector
import nz.net.wand.streamevmon.flink.sinks.InfluxSinkFunction
import nz.net.wand.streamevmon.flink.sources.AmpMeasurementSourceFunction
import nz.net.wand.streamevmon.measurements._

import java.time.Duration

import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala._

/** Main runner for loss detector, detailed in the
  * [[nz.net.wand.streamevmon.detectors.loss]] package.
  */
object LossRunner {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("source.influx.subscriptionName", "LossDetector")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env.disableOperatorChaining

    env.enableCheckpointing(Duration.ofSeconds(10).toMillis, CheckpointingMode.EXACTLY_ONCE)

    val source = env
      .addSource(new AmpMeasurementSourceFunction)
      .setParallelism(1)
      .name("Measurement Subscription")
      .uid("loss-measurement-sourcefunction")
      .keyBy(new MeasurementKeySelector[InfluxMeasurement])

    val detector = new LossDetector[InfluxMeasurement]

    val process = source
      .process(detector)
      .name(detector.flinkName)
      .uid("loss-detector")

    process
      .addSink(new InfluxSinkFunction)
      .name("Influx Sink")
      .uid("loss-influx-sink")

    process.print("Loss Event")

    env.execute("Measurement subscription -> Loss Detector")
  }
}
