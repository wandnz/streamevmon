package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.changepoint.{ChangepointDetector, NormalDistribution}
import nz.net.wand.streamevmon.detectors.distdiff.DistDiffDetector
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.detectors.spike.SpikeDetector
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.{LatencyTSAmpFileInputFormat, MeasurementKeySelector}
import nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP

import java.io.File

import org.apache.commons.io.FilenameUtils
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.scala.operators.ScalaCsvOutputFormat
import org.apache.flink.core.fs.Path
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._

import scala.reflect.io.Directory

object LatencyTSAllDetectors {

  def runTest(file: File): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val config = Configuration.get()
    env.getConfig.setGlobalJobParameters(config)

    env.disableOperatorChaining

    env.setParallelism(1)

    val keySelector = new MeasurementKeySelector[LatencyTSAmpICMP]

    lazy val inputFormat = new LatencyTSAmpFileInputFormat

    val input = env
      .readFile(inputFormat, file.getCanonicalPath)
      .keyBy(keySelector)

    implicit val normalDistributionTypeInformation: TypeInformation[NormalDistribution[LatencyTSAmpICMP]] =
      TypeInformation.of(classOf[NormalDistribution[LatencyTSAmpICMP]])

    val detectors = Seq(
      new ChangepointDetector[LatencyTSAmpICMP, NormalDistribution[LatencyTSAmpICMP]](
        new NormalDistribution[LatencyTSAmpICMP](mean = 0)
      ),
      new DistDiffDetector[LatencyTSAmpICMP],
      new ModeDetector[LatencyTSAmpICMP],
      new SpikeDetector[LatencyTSAmpICMP]
    )
    val outputFormat = new ScalaCsvOutputFormat[(String, Int, Long, String)](
      new Path(s"./out/allDetectors/${FilenameUtils.removeExtension(file.getName)}.events.csv")
    )

    val detectorsWithSource = detectors.map { det =>
      input.process(det)
    }
    val detectorsAsOne = detectorsWithSource.head.union(detectorsWithSource.drop(1): _*)

    val eventsAsTuple = detectorsAsOne
      .map { e =>
        val tuple = Event.unapply(e).get
        (
          tuple._1,
          tuple._3,
          tuple._4.toEpochMilli,
          tuple._6
        )
      }
    eventsAsTuple.writeUsingOutputFormat(outputFormat)
    eventsAsTuple.print()

    env.execute()
  }

  def main(args: Array[String]): Unit = {
    new Directory(new File("./out/allDetectors")).deleteRecursively()

    new File("data/latency-ts-i/ampicmp/series/")
      .listFiles(_.getName.endsWith(".series"))
      .foreach(runTest)
  }
}
