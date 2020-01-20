package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.flink.{LatencyTSAmpFileInputFormat, MeasurementKeySelector}
import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.distdiff.DistDiffDetector
import nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP

import java.time.Duration

import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala._

object DistDiffRunner {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("influx.dataSource.subscriptionName", "DistDiffDetector")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env.disableOperatorChaining

    env.enableCheckpointing(Duration.ofSeconds(10).toMillis, CheckpointingMode.EXACTLY_ONCE)

    env.setParallelism(1)
    env.setMaxParallelism(1)

    val source = env
      .readFile(new LatencyTSAmpFileInputFormat, "data/latency-ts-i/ampicmp/series/waikato-xero-ipv4.series")
      .setParallelism(1)
      .name("Latency TS AMP ICMP")
      .uid("distdiff-source")
      .filter(_.lossrate == 0.0)
      .name("Has data?")
      .keyBy(new MeasurementKeySelector[LatencyTSAmpICMP])

    val detector = new DistDiffDetector[LatencyTSAmpICMP]

    val process = source
      .process(detector)
      .name(detector.detectorName)
      .uid("distdiff-detector")
      .setParallelism(1)

    process.print("distdiff-printer")

    env.execute("Latency TS AMP ICMP -> Dist Diff Detector")
  }
}
