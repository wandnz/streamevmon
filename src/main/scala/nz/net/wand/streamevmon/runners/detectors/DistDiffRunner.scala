package nz.net.wand.streamevmon.runners.detectors

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.distdiff._
import nz.net.wand.streamevmon.flink.sources.LatencyTSAmpFileInputFormat
import nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP
import nz.net.wand.streamevmon.measurements.MeasurementKeySelector

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow

object DistDiffRunner {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("source.influx.subscriptionName", "DistDiffDetector")

    val config = Configuration.get(args)
    env.getConfig.setGlobalJobParameters(config)

    env.disableOperatorChaining

    env.setParallelism(1)
    env.setMaxParallelism(1)

    val source = env
      .readFile(new LatencyTSAmpFileInputFormat, "data/latency-ts-i/ampicmp/series/waikato-xero-ipv4.series")
      .setParallelism(1)
      .name("Latency TS AMP ICMP")
      .uid("distdiff-source")
      .filter(_.defaultValue.nonEmpty)
      .name("Has data?")
      .keyBy(new MeasurementKeySelector[LatencyTSAmpICMP])

    lazy val window = source.countWindow(config.getInt("detector.distdiff.recentsCount") * 2, 1)

    val process = if (config.getBoolean("detector.distdiff.useFlinkWindow")) {
      val detector = new WindowedDistDiffDetector[LatencyTSAmpICMP, GlobalWindow]
      window
        .process(detector)
        .name(detector.flinkName)
    }
    else {
      {
        val detector = new DistDiffDetector[LatencyTSAmpICMP]
        source
          .process(detector)
          .name(detector.flinkName)
        }
        .uid("distdiff-detector")
        .setParallelism(1)
    }
    process.print("distdiff-printer")

    env.execute("Latency TS AMP ICMP -> Dist Diff Detector")
  }
}
