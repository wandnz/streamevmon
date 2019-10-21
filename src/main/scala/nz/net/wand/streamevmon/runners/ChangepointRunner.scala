package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.flink.LatencyTSAmpFileInputFormat
import nz.net.wand.streamevmon.measurements.LatencyTSAmpICMP
import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.changepoint._

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic

object ChangepointRunner {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env.disableOperatorChaining

    val filename = "data/latency-ts-i/ampicmp/series/waikato-xero-ipv4.series"

    val source = env
      .readFile(new LatencyTSAmpFileInputFormat, filename)
      //.readFile(new LatencyTSAmpFileInputFormat, "data/latency-ts-i/ampicmp/waikato-xero-ipv4.series.small")
      .name("Latency TS I AMP ICMP Parser")
      .setParallelism(1)
      .keyBy(_.stream)

    val detector = new ChangepointDetector
                         [LatencyTSAmpICMP, NormalDistribution[LatencyTSAmpICMP]](
      new NormalDistribution(mean = 0, mapFunction = _.average)
    )

    val process = source
      .process(detector)
      .name(detector.detectorName)
      .setParallelism(1)

    process.print(s"${getClass.getSimpleName} sink")

    env.execute("Latency TS I AMP ICMP -> Changepoint Detector")
  }
}
