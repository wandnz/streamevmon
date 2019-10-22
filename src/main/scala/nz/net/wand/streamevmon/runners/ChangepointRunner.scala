package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.flink.LatencyTSAmpFileInputFormat
import nz.net.wand.streamevmon.measurements.LatencyTSAmpICMP
import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.changepoint._

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic

/** This is the main runner for the changepoint detector, which is
  * found in the [[nz.net.wand.streamevmon.detectors.changepoint]] package.
  *
  * This runner uses the [[nz.net.wand.streamevmon.flink.LatencyTSAmpFileInputFormat LatencyTSAmpFileInputFormat]],
  * which must be supplied with files from the Latency TS I dataset.
  *
  * @see [[nz.net.wand.streamevmon.detectors.changepoint the package description]] for details.
  * @see [[nz.net.wand.streamevmon.detectors.changepoint.ChangepointGraphs ChangepointGraphs]] for an alternative bulk runner.
  */
object ChangepointRunner {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env.disableOperatorChaining

    val filename = "data/latency-ts-i/ampicmp/series/waikato-xero-ipv4.series"

    val source = env
      .readFile(new LatencyTSAmpFileInputFormat, filename)
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
