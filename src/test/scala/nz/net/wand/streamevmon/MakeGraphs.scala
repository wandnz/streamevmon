package nz.net.wand.streamevmon

import nz.net.wand.streamevmon.detectors.changepoint.{ChangepointDetector, NormalDistribution}
import nz.net.wand.streamevmon.flink.LatencyTSAmpFileInputFormat
import nz.net.wand.streamevmon.measurements.LatencyTSAmpICMP

import java.io.File

import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.apache.flink.streaming.api.TimeCharacteristic
import org.scalatest.WordSpec

class MakeGraphs extends WordSpec {

  "Graphs" when {
    "ChangepointProcessor" when {

      def doIt(file: String, maxhist: Int, triggerCount: Int, severity: Int): Unit = {
        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

        val conf = Array(
          "--detector.changepoint.maxHistory", s"$maxhist",
          "--detector.changepoint.triggerCount", s"$triggerCount",
          "--detector.changepoint.severityThreshold", s"$severity"
        )

        env.getConfig.setGlobalJobParameters(Configuration.get(conf))

        env.disableOperatorChaining

        val source = env
          .readFile(
            new LatencyTSAmpFileInputFormat,
            file
          )
          .name("Latency TS I AMP ICMP Parser")
          .setParallelism(1)
          .filter(_.lossrate == 0.0)
          .keyBy(_.stream)

        val detector =
          new ChangepointDetector[LatencyTSAmpICMP, NormalDistribution[LatencyTSAmpICMP]](
            new NormalDistribution[LatencyTSAmpICMP](mean = 0, mapFunction = _.average),
            shouldDoGraphs = true,
            Some(file)
          )

        val process = source
          .process(detector)
          .name(detector.detectorName)

        process.print(s"${getClass.getSimpleName} sink")

        env.execute("Latency TS I AMP ICMP -> Changepoint Detector")
      }

      def getListOfFiles(dir: String): Seq[String] = {
        val file = new File(dir)
        file.listFiles.filter(_.isFile)
          .map(_.getPath).toList
      }

      "all" in {
        for (file <- getListOfFiles("data/latency-ts-i/ampicmp/series")) {
          for (maxhist <- Seq(20, 40, 60)) {
            for (triggerCount <- Seq(10, 20, 30, 40)) {
              for (severity <- Seq(15, 20, 25, 30)) {
                doIt(file, maxhist, triggerCount, severity)
              }
            }
          }
        }
      }
    }
  }
}
