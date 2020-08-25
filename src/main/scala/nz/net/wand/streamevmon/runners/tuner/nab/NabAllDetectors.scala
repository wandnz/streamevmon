package nz.net.wand.streamevmon.runners.tuner.nab

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.baseline.BaselineDetector
import nz.net.wand.streamevmon.detectors.changepoint.{ChangepointDetector, NormalDistribution}
import nz.net.wand.streamevmon.detectors.distdiff.DistDiffDetector
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.detectors.spike.SpikeDetector
import nz.net.wand.streamevmon.flink.MeasurementKeySelector
import nz.net.wand.streamevmon.flink.sources.NabFileInputFormat
import nz.net.wand.streamevmon.measurements.nab.NabMeasurement

import java.io.File
import java.nio.file.{Files, Paths}

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._

import scala.reflect.io.Directory

/** Runs all detectors against each series in the Latency TS I dataset.
  *
  * Each file is run separately so that we can more explicitly put the results
  * in the correct files. This could be done with a single Flink pipeline, but
  * using the StreamingFileSink with finite sources is a pain, and naming the
  * files how they should be is a lot more verbose. This is a simpler and nicer
  * solution that just adds a bit of extra overhead to create the pipeline for
  * each file.
  *
  * These results are later processed in Python to get proper NAB results.
  */
object NabAllDetectors {

  def main(args: Array[String]): Unit = {
    new NabAllDetectors().runOnAllNabFiles(args, "./out/nab-allDetectors")
  }
}

class NabAllDetectors {

  def runTest(args: Array[String], file: File, outputDir: String): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val config = Configuration.get(args)
    env.getConfig.setGlobalJobParameters(config)

    env.disableOperatorChaining

    type MeasT = NabMeasurement

    // Our detectors are all keyed so that data goes to the right place in
    // environments where there's multiple streams.
    val keySelector = new MeasurementKeySelector[MeasT]

    // We do single parallelism just to make sure everything goes in the same
    // order. This is probably not needed, but that's fine.
    val input = env
      .readFile(new NabFileInputFormat, file.getCanonicalPath)
      .setParallelism(1)
      .keyBy(keySelector)

    implicit val normalDistributionTypeInformation: TypeInformation[NormalDistribution[MeasT]] =
      TypeInformation.of(classOf[NormalDistribution[MeasT]])

    // List all the detectors we want to use.
    val detectors = Seq(
      new BaselineDetector[MeasT],
      new ChangepointDetector[MeasT, NormalDistribution[MeasT]](
        new NormalDistribution[MeasT](mean = 0)
      ),
      new DistDiffDetector[MeasT],
      new ModeDetector[MeasT],
      new SpikeDetector[MeasT],
    )

    // We use every detector as a ProcessFunction
    val detectorsWithSource = detectors.map { det =>
      input.process(det)
    }

    // And we write their results out to file in the NAB scoring format.
    detectors.zip(detectorsWithSource).map {
      case (det, stream) => stream.addSink(
        new NabScoringFormatSink(
          s"$outputDir/${det.configKeyGroup}",
          file,
          det.configKeyGroup
        )
      )
    }

    env.execute()
  }

  def runOnAllNabFiles(args: Array[String], outputDir: String): Unit = {
    // Delete the existing outputs so it doesn't append when we don't want it to.
    new Directory(new File(outputDir)).deleteRecursively()

    // Use all the series files, making sure we don't get anything else like
    // .events files or READMEs.
    Files.walk(Paths.get("./data/NAB/data"))
      .filter(_.toFile.isFile)
      .filter(_.toFile.getName.endsWith(".csv"))
      .forEach { f =>
        println(f)
        runTest(args, f.toFile, outputDir)
      }
  }
}
