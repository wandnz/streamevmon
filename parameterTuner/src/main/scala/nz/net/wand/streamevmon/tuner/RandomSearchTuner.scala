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
import nz.net.wand.streamevmon.parameters.{HasParameterSpecs, Parameters}
import nz.net.wand.streamevmon.runners.unified.schema.DetectorType
import nz.net.wand.streamevmon.tuner.jobs.{FailedJob, JobResult, SimpleJob}
import nz.net.wand.streamevmon.tuner.nab.{NabJob, NabJobResult}
import nz.net.wand.streamevmon.tuner.strategies.{RandomSearch, SearchStrategy}

import java.nio.file.{Files, Paths}

/** This is an entrypoint that performs parameter tuning by random search.
  */
object RandomSearchTuner extends Logging {

  /** Generate new parameters until we find a valid set, then start a job for
    * those parameters.
    */
  def queueNewJob(
    strategy: SearchStrategy
  ): Unit = {
    var params: Parameters = null
    var outputPath = "."
    var paramsAreValid = false

    // Keep on generating new parameters until we find one that's not been
    // tested before -- it's very unlikely that we'll hit a duplicate
    // considering the number of parameters and the range they can span.
    // We also need to check that the parameters are actually valid.
    while (!paramsAreValid || Files.exists(Paths.get(outputPath))) {
      params = strategy.nextParameters()
      paramsAreValid = HasParameterSpecs.parameterInstancesAreValid(params.elems)
      outputPath = s"./out/parameterTuner/random/${params.hashCode.toString}"
    }

    // Spin the job off. It'll come back eventually.
    ConfiguredPipelineRunner.submit(
      NabJob(
        params,
        outputPath,
        detectors = Seq(DetectorType.Baseline)
      ))
  }

  def main(args: Array[String]): Unit = {

    // Squash all the logs from Flink to tidy up our output.
    System.setProperty("org.slf4j.simpleLogger.log.org.apache.flink", "error")
    System.setProperty("nz.net.wand.streamevmon.tuner.nabScoreScalingMode", "continuous")

    // Let's make a random search strategy. Some parameters are fixed, like the
    // inactivityPurgeTimes.
    val searchStrategy = RandomSearch(
      HasParameterSpecs.getAllDetectorParameters,
      HasParameterSpecs.fixedParameters
    )

    // When we get a job result, we should write the scores into a results
    // document and queue up a new job with new parameters.
    ConfiguredPipelineRunner.addJobResultHook { jr: JobResult => {
      logger.info(s"Got job result! $jr")
      jr match {
        case NabJobResult(_, results) =>
          logger.debug(results.toString)
          queueNewJob(searchStrategy)
        case FailedJob(_, e) =>
          logger.error(s"Job failed with $e")
          queueNewJob(searchStrategy)
        case _ => logger.info("Ending job loop.")
      }
    }
    }

    // We'll send a hello world job to make sure everything's working right...
    ConfiguredPipelineRunner.submit(SimpleJob("HelloWorld"))
    // and then queue our first random job. This process will never exit, and
    // the logs must be inspected manually to find the best-performing result.
    queueNewJob(searchStrategy)
  }
}
