/* This file is part of streamevmon.
 *
 * Copyright (C) 2020-2021  The University of Waikato, Hamilton, New Zealand
 *
 * Author: Daniel Oosterwijk
 *
 * All rights reserved.
 *
 * This code has been developed by the University of Waikato WAND
 * research group. For further information please see https://wand.nz,
 * or our Github organisation at https://github.com/wanduow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.net.wand.streamevmon.tuner.nab

import nz.net.wand.streamevmon.{Configuration, Logging}
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.flink.sources.NabFileInputFormat
import nz.net.wand.streamevmon.measurements.nab.NabMeasurement
import nz.net.wand.streamevmon.measurements.MeasurementKeySelector
import nz.net.wand.streamevmon.runners.unified.schema.DetectorType

import java.io.File
import java.nio.file.{Files, Paths}

import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._

import scala.compat.java8.StreamConverters._
import scala.reflect.io.Directory

object NabAllDetectors {

  /** Simple entrypoing to run all detectors against NAB.
    */
  def main(args: Array[String]): Unit = {
    new NabAllDetectors(Seq(
      DetectorType.Baseline,
      DetectorType.Changepoint,
      DetectorType.DistDiff,
      DetectorType.Mode,
      DetectorType.Spike
    )).runOnAllNabFiles(args, "./out/nab-allDetectors", deleteOutputDirectoryBeforeStart = true)
  }
}

/** Runs all detectors against each series in the NAB dataset.
  *
  * Each file is run separately so that we can more explicitly put the results
  * in the correct files. This could be done with a single Flink pipeline, but
  * using the StreamingFileSink with finite sources is a pain, and naming the
  * files how they should be is a lot more verbose. This is a simpler and nicer
  * solution that just adds a bit of extra overhead to create the pipeline for
  * each file.
  *
  * These results should be later processed in Python to get proper NAB results.
  */
class NabAllDetectors(detectorsToUse: Iterable[DetectorType.ValueBuilder]) extends Logging {

  /** Runs the detectors against a single NAB file with a fresh Flink pipeline.
    */
  def runTest(args: Array[String], file: File, outputDir: String): Unit = {

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)

    // Configuration is overridden by parameters passed as though through the
    // command line.
    val config = Configuration.get(args)
    env.getConfig.setGlobalJobParameters(config)

    // Our detectors are all keyed so that data goes to the right place in
    // environments where there's multiple streams. In this case there's not,
    // but we need to select keys anyway.
    val keySelector = new MeasurementKeySelector[NabMeasurement]

    // We do single parallelism just to make sure everything goes in the same
    // order. This is probably not needed, but that's fine.
    val input = env
      .readFile(new NabFileInputFormat, file.getCanonicalPath)
      .keyBy(keySelector)

    // List all the detectors we want to use.
    val detectors = detectorsToUse
      .map(_.buildKeyed[NabMeasurement])
      .asInstanceOf[Iterable[KeyedProcessFunction[String, NabMeasurement, Event] with HasFlinkConfig]]

    // Apply them all to the input measurement stream.
    val detectorsWithSource = detectors.map { det =>
      input.process(det)
    }

    // Write their results out to file in the NAB scoring format.
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

  /** Runs the specified detectors against the entire NAB dataset. This will
    * make a separate Flink pipeline for every file.
    */
  def runOnAllNabFiles(args: Array[String], outputDir: String, deleteOutputDirectoryBeforeStart: Boolean): Unit = {
    if (deleteOutputDirectoryBeforeStart) {
      // Delete the existing outputs so it doesn't append when we don't want it to.
      new Directory(new File(outputDir)).deleteRecursively()
    }

    // Use all the data files, making sure we don't get anything else like READMEs.
    val files = Files.walk(Paths.get("./data/NAB/data"))
      .filter(_.toFile.isFile)
      .filter(_.toFile.getName.endsWith(".csv"))
      .toScala[Stream]

    files
      .zipWithIndex
      .foreach { case (f, i) =>
        logger.debug(s"Processing file #${i + 1}/${files.size} - $f")
        runTest(args, f.toFile, outputDir)
      }
  }
}
