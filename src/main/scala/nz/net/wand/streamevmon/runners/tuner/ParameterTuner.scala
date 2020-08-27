package nz.net.wand.streamevmon.runners.tuner

import nz.net.wand.streamevmon.runners.tuner.jobs.{FailedJob, JobResult, SimpleJob}
import nz.net.wand.streamevmon.runners.tuner.nab.{NabJob, NabJobResult}
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.runners.tuner.parameters.{DetectorParameterSpecs, Parameters}
import nz.net.wand.streamevmon.runners.tuner.strategies.{RandomSearch, SearchStrategy}

import java.io._
import java.nio.file.{Files, Paths}

object ParameterTuner extends Logging {

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

    // We'll write down the parameters in a new directory. When the job
    // finishes, we'll also write the results in this directory.
    Files.createDirectories(Paths.get(outputPath))
    val parameterWriter = new BufferedWriter(new FileWriter(s"$outputPath/params.properties"))
    params.elems.sortBy(_.name).foreach {
      p =>
        parameterWriter.write(s"${p.name}=${p.value}")
        parameterWriter.newLine()
    }
    parameterWriter.flush()
    parameterWriter.close()

    // We'll also serialize the Parameters object so we don't have to bother
    // parsing a properties file later.
    val oos = new ObjectOutputStream(new FileOutputStream(s"$outputPath/params.spkl"))
    oos.writeObject(params)
    oos.close()

    // Spin the job off. It'll come back eventually.
    ConfiguredPipelineRunner.submit(NabJob(
      params,
      outputPath,
      detectors = NabJob.allDetectors,
      skipDetectors = false,
      skipScoring = false
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
    ConfiguredPipelineRunner.addJobResultHook {
      jr: JobResult => {
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
