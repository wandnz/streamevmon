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

package nz.net.wand.streamevmon.tuner

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.tuner.nab.smac.NabTAEFactory

import java.io._
import java.time.Instant

import ca.ubc.cs.beta.smac.executors.SMACExecutor
import org.apache.commons.io.output.TeeOutputStream

/** Main entrypoint for parameterTuner module. Optimises detector algorithms
  * for quality on the NAB dataset and its associated scorer, using SMAC as an
  * optimisation driver.
  */
object ParameterTuner extends Logging {

  // These control where the output files go. The smacdir contains all run
  // outputs, including the NAB scoring artifacts in the run-outputs/ subfolder.
  val baseOutputDir = "out/parameterTuner"
  val smacdir = "smac"

  def main(args: Array[String]): Unit = {
    // If we squash a lot of the logs, the output is a lot tidier. We don't need
    // incremental progress updates most of the time, especially not for each
    // individual file being processed.
    System.setProperty("org.slf4j.simpleLogger.log.org.apache.flink", "error")
    System.setProperty(
      "org.slf4j.simpleLogger.log.nz.net.wand.streamevmon.tuner.nab.NabAllDetectors",
      "error"
    )
    //System.setProperty("org.slf4j.simpleLogger.log.nz.net.wand.streamevmon.tuner.nab.smac", "info")
    // We'll also slap a timestamp on the front of the log for a bit more detail.
    System.setProperty("org.slf4j.simpleLogger.showDateTime", "true")
    System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd'T'HH:mm:ss")

    // Parse command line arguments and grab some initial options.
    val opts = ProgramOptions(args)
    val detectorsToUse = opts.getDetectors
    val scoreTargets = opts.getScoreTargets

    if (detectorsToUse.isEmpty) {
      throw new IllegalArgumentException("Can't start without any detectors")
    }
    if (scoreTargets.isEmpty) {
      throw new IllegalArgumentException("Can't start without any score targets")
    }

    // The rungroup is used internally by SMAC, and we also use it as a unique
    // folder name for this execution of the parameterTuner.
    val rungroup =
    s"${detectorsToUse.mkString(",")}-" +
      s"${scoreTargets.mkString(",")}-" +
      s"${Instant.ofEpochMilli(System.currentTimeMillis())}"

    // Set up some global config. Since SMAC creates its own copy of all the
    // objects we actually use at runtime, we unfortunately need to pass
    // arguments to them in a global fashion.
    System.setProperty("nz.net.wand.streamevmon.tuner.detectors", detectorsToUse.mkString(","))
    System.setProperty("nz.net.wand.streamevmon.tuner.scoreTargets", scoreTargets.mkString(","))
    System.setProperty("nz.net.wand.streamevmon.tuner.runOutputDir", s"$baseOutputDir/$smacdir/$rungroup/run-outputs")
    System.setProperty("nz.net.wand.streamevmon.tuner.pythonProfileParameter", opts.doPythonProfilingKeyword)
    System.setProperty("nz.net.wand.streamevmon.tuner.cleanupNabOutputs", opts.cleanupNabScorerOutputs.toString)
    System.setProperty("nz.net.wand.streamevmon.tuner.nabScoreScalingMode", opts.nabScoreScalingMode.toString)

    // SMAC needs to know about valid parameters and parameter restrictions.
    val parameterSpecFile = s"$baseOutputDir/$smacdir/$rungroup/parameterspec.smac"
    ParameterSpecToSmac.populateSmacParameterSpec(
      parameterSpecFile,
      opts.randomiseDefaults,
      detectorsToUse: _*
    )

    // We'll tee our stdout to a file, so we can review it later as well as
    // seeing it in the terminal as usual.
    System.setOut(
      new PrintStream(
        new TeeOutputStream(
          new FileOutputStream(s"$baseOutputDir/$smacdir/$rungroup/stdout.log"),
          new FileOutputStream(FileDescriptor.out)
        )
      )
    )

    logger.info(s"Using detectors ${detectorsToUse.mkString("(", ", ", ")")} and score targets ${scoreTargets.mkString("(", ", ", ")")}")

    // Start SMAC directly - no subprocesses here.
    SMACExecutor.oldMain(
      Array(
        // The TAEFactory generates our Target Algorithm Executor.
        "--tae", new NabTAEFactory().getName,
        // Since we're using a custom TAE, we don't have a command string to execute the algorithm with.
        "--algo-exec", "",
        // We want to optimise for quality, not runtime.
        "--run-obj", "QUALITY",
        // Let SMAC know where our parameter spec file is.
        "--pcs-file", parameterSpecFile,
        // Since we handle input files ourselves, we don't need SMAC to bother with an instance file.
        "--use-instances", "false",
        // These all set names for output directories and some internal IDs.
        "--experiment-dir", baseOutputDir,
        "--output-dir", smacdir,
        "--rungroup", rungroup,
        // If any non-default execution limits were provided, they'll take effect here.
        "--cputime-limit", opts.cputimeLimit.toString,
        "--iteration-limit", opts.iterationLimit.toString,
        "--wallclock-limit", opts.wallclockLimit.toString,
        "--runcount-limit", opts.runcountLimit.toString,
        // The validation run that occurs at the end of a SMAC execution can be disabled.
        "--doValidation", opts.doValidation.toString,
        // If an execution crashes, like if the scorer fails, we can try again a number of times.
        "--retry-crashed-count", opts.retryCrashedCount.toString
      ))

    logger.info("SMAC finished. Shutting down ActorSystem.")
    ConfiguredPipelineRunner.shutdownImmediately()
    System.exit(0)
  }
}
