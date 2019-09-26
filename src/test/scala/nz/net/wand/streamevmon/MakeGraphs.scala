package nz.net.wand.streamevmon

import nz.net.wand.streamevmon.detectors.changepoint.{ChangepointDetector, NormalDistribution}
import nz.net.wand.streamevmon.flink.LatencyTSAmpFileInputFormat
import nz.net.wand.streamevmon.measurements.LatencyTSAmpICMP

import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.apache.flink.streaming.api.TimeCharacteristic
import org.scalatest.WordSpec

class MakeGraphs extends WordSpec {

  "Graphs" when {
    "ChangepointProcessor" when {

      def doIt(normalise: Boolean, squash: Boolean): Unit = {
        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

        env.getConfig.setGlobalJobParameters(Configuration.get(Array()))

        env.disableOperatorChaining

        val source = env
          .readFile(
            new LatencyTSAmpFileInputFormat,
            //"data/latency-ts-i/ampicmp/waikato-xero-ipv4.series.small"
            "data/latency-ts-i/ampicmp/waikato-xero-ipv4.series.change"
            //"data/latency-ts-i/ampicmp/series/waikato-xero-ipv4.series"
          )
          .name("Latency TS I AMP ICMP Parser")
          .setParallelism(1)
          .keyBy(_.stream)

        val detector =
          new ChangepointDetector[LatencyTSAmpICMP, NormalDistribution[LatencyTSAmpICMP]](
            new NormalDistribution[LatencyTSAmpICMP](mapFunction = _.average),
            normalise,
            squash
          )

        val process = source
          .process(detector)
          .name(detector.detectorName)
          .setParallelism(1)

        process.print(s"${getClass.getSimpleName} sink")

        env.execute("Latency TS I AMP ICMP -> Changepoint Detector")
      }

      "all" in {
        for (normalise <- Seq(true, false)) {
          for (squash <- Seq(true)) {
            doIt(normalise, squash)
          }
        }
      }
    }
  }
}
