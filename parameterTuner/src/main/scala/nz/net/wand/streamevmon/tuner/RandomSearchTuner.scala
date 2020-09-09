package nz.net.wand.streamevmon.tuner

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.parameters.{DetectorParameterSpecs, ParameterInstance, Parameters}
import nz.net.wand.streamevmon.runners.unified.schema.DetectorType
import nz.net.wand.streamevmon.tuner.jobs.{FailedJob, JobResult, SimpleJob}
import nz.net.wand.streamevmon.tuner.nab.{NabJob, NabJobResult}
import nz.net.wand.streamevmon.tuner.strategies.{RandomSearch, SearchStrategy}

import java.nio.file.{Files, Paths}

object RandomSearchTuner extends Logging {

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
      paramsAreValid = DetectorParameterSpecs.parametersAreValid(params.elems)
      outputPath = s"./out/parameterTuner/random/${params.hashCode.toString}"
    }

    params = new Parameters(
      ParameterInstance(
        DetectorParameterSpecs.getAllDetectorParameters
          .find(_.name == "detector.baseline.maxHistory")
          .get,
        10
      ),
      ParameterInstance(
        DetectorParameterSpecs.getAllDetectorParameters
          .find(_.name == "detector.baseline.percentile")
          .get,
        0.0768485816479999
      ),
      ParameterInstance(
        DetectorParameterSpecs.getAllDetectorParameters
          .find(_.name == "detector.baseline.threshold")
          .get,
        68
      )
    )
    outputPath = s"./out/parameterTuner/random/${params.hashCode.toString}"

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

    // Let's make a random search strategy. Some parameters are fixed, like the
    // inactivityPurgeTimes.
    val searchStrategy = RandomSearch(
      DetectorParameterSpecs.getAllDetectorParameters,
      DetectorParameterSpecs.fixedParameters
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

    ConfiguredPipelineRunner.submit(SimpleJob("HelloWorld"))
    queueNewJob(searchStrategy)
  }
}
