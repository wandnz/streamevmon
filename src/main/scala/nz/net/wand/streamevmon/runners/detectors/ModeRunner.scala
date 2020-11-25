package nz.net.wand.streamevmon.runners.detectors

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.flink.sources.LatencyTSAmpFileInputFormat
import nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP
import nz.net.wand.streamevmon.measurements.MeasurementKeySelector

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._

/** Main runner for mode change detector, detailed in the
  * [[nz.net.wand.streamevmon.detectors.mode]] package.
  */
object ModeRunner {
  def main(args: Array[String]): Unit = {
    val filename = "data/latency-ts-i/ampicmp/series/waikato-xero-ipv4.series"
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("source.influx.subscriptionName", "ModeDetector")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env.disableOperatorChaining

    val source = env
      .readFile(new LatencyTSAmpFileInputFormat, filename)
      .setParallelism(1)
      .name("Latency TS AMP Input")
      .keyBy(new MeasurementKeySelector[LatencyTSAmpICMP])

    val detector = new ModeDetector[LatencyTSAmpICMP]

    val process = source
      .process(detector)
      .name(detector.flinkName)
      .uid(detector.flinkUid)

    process.print(s"Mode Event ($filename)")

    env.execute("Measurement subscription -> Mode Detector")
  }
}
