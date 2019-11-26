package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.flink.LatencyTSAmpFileInputFormat
import nz.net.wand.streamevmon.measurements._

import java.time.Duration

import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala._

/** Main runner for mode change detector, detailed in the
  * [[nz.net.wand.streamevmon.detectors.mode]] package.
  */
object ModeRunner {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("influx.dataSource.subscriptionName", "ModeDetector")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env.disableOperatorChaining

    env.enableCheckpointing(Duration.ofSeconds(10).toMillis, CheckpointingMode.EXACTLY_ONCE)

    val source = env
    /*
      .addSource(new MeasurementSourceFunction)
      .setParallelism(1)
      .name("Measurement Subscription")
      .uid("mode-measurement-sourcefunction")
       */
      .readFile(new LatencyTSAmpFileInputFormat,
                "data/latency-ts-i/ampicmp/series/waikato-xero-ipv4.series")
      .setParallelism(1)
      .name("Latency TS AMP Input")
      .keyBy(_.stream)

    val detector = new ModeDetector[LatencyTSAmpICMP]

    val process = source
      .process(detector)
      .name(detector.detectorName)
      .uid("mode-detector")

    /*
    process
      .addSink(new InfluxSinkFunction)
      .name("Influx Sink")
      .uid("mode-influx-sink")
     */

    process.print("Mode Event")

    env.execute("Measurement subscription -> Mode Detector")
  }
}
