package nz.net.wand.streamevmon.detectors.changepoint

import nz.net.wand.streamevmon.flink.{LatencyTSAmpFileInputFormat, MeasurementKeySelector}
import nz.net.wand.streamevmon.measurements.LatencyTSAmpICMP
import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.MapFunction

import java.io.File

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.apache.flink.streaming.api.TimeCharacteristic

/** This class is an alternative runner for the changepoint detector that allows
  * iteration over configuration changes and various input files. It also has
  * the graphing options turned on, meaning some .csv files will be output into
  * ./out/graphs
  */
object ChangepointGraphs {

  def doIt(file: String, maxhist: Int, triggerCount: Int, severity: Int): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val conf = Array(
      "--detector.changepoint.maxHistory",
      s"$maxhist",
      "--detector.changepoint.triggerCount",
      s"$triggerCount",
      "--detector.changepoint.severityThreshold",
      s"$severity"
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
      .keyBy(new MeasurementKeySelector[LatencyTSAmpICMP])

    implicit val ti: TypeInformation[NormalDistribution[LatencyTSAmpICMP]] = TypeInformation.of(classOf[NormalDistribution[LatencyTSAmpICMP]])

    case class TsIcmpToAverage() extends MapFunction[LatencyTSAmpICMP, Double] {
      override def apply(t: LatencyTSAmpICMP): Double = t.average
    }

    val detector =
      new ChangepointDetector[LatencyTSAmpICMP, NormalDistribution[LatencyTSAmpICMP]](
        new NormalDistribution[LatencyTSAmpICMP](
          mean = 0,
          mapFunction = new TsIcmpToAverage
        ),
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
    file.listFiles
      .filter(_.isFile)
      .map(_.getPath)
      .toList
  }

  def main(args: Array[String]): Unit = {
    for (file <- getListOfFiles("data/latency-ts-i/ampicmp/series")) {
      doIt(file, 20, 10, 30)

      /*
      for (maxhist <- Seq(20, 40, 60)) {
        for (triggerCount <- Seq(10, 20, 30, 40)) {
          for (severity <- Seq(15, 20, 25, 30)) {
            doIt(file, maxhist, triggerCount, severity)
          }
        }
      }

     */
    }
  }
}
