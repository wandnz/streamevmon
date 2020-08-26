package nz.net.wand.streamevmon.runners.tuner.nab

import nz.net.wand.streamevmon.{Configuration, Logging}
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.{HasFlinkConfig, MeasurementKeySelector}
import nz.net.wand.streamevmon.flink.sources.NabFileInputFormat
import nz.net.wand.streamevmon.measurements.nab.NabMeasurement
import nz.net.wand.streamevmon.runners.unified.schema.DetectorType

import java.io.File
import java.nio.file.{Files, Paths}

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._

import scala.compat.java8.StreamConverters._
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
    new NabAllDetectors(Seq(
      DetectorType.Baseline,
      DetectorType.Changepoint,
      DetectorType.DistDiff,
      DetectorType.Mode,
      DetectorType.Spike
    )).runOnAllNabFiles(args, "./out/nab-allDetectors")
  }
}

class NabAllDetectors(detectorsToUse: Iterable[DetectorType.ValueBuilder]) extends Logging {

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

    // List all the detectors we want to use.
    val detectors = detectorsToUse
      .map(_.buildKeyed[NabMeasurement])
      .asInstanceOf[Iterable[KeyedProcessFunction[String, NabMeasurement, Event] with HasFlinkConfig]]

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
    val files = Files.walk(Paths.get("./data/NAB/data"))
      .filter(_.toFile.isFile)
      .filter(_.toFile.getName.endsWith(".csv"))
      .toScala[Stream]

    files
      .zipWithIndex
      .foreach { case (f, i) =>
        logger.info(s"Processing file #${i + 1}/${files.size} - $f")
        runTest(args, f.toFile, outputDir)
      }
  }
}
