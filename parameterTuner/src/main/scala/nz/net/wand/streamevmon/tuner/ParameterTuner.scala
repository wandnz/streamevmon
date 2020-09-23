package nz.net.wand.streamevmon.tuner

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.parameters.{DetectorParameterSpecs, Parameters}
import nz.net.wand.streamevmon.runners.unified.schema.DetectorType
import nz.net.wand.streamevmon.tuner.nab.smac.NabTAEFactory

import java.io._
import java.time.Instant

import ca.ubc.cs.beta.smac.executors.SMACExecutor
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.commons.io.FileUtils
import org.apache.commons.io.output.TeeOutputStream

import scala.util.{Random, Try}

/** Main entrypoint for parameterTuner module. Optimises detector algorithms
  * for quality on the NAB dataset and its associated scorer, using SMAC as an
  * optimisation driver.
  */
object ParameterTuner extends Logging {

  // These control where the output files go. The smacdir contains all run
  // outputs, including the NAB scoring artifacts in the run-outputs/ subfolder.
  val baseOutputDir = "out/parameterTuner"
  val smacdir = "smac"

  /** Unused method to get scores from previous runs that are still on disk.
    * This could potentially be used in the future to warm-start SMAC, though
    * it does keep its own records of checkpoints and such.
    */
  def getHistoricalResults: Seq[(Map[String, Map[String, Double]], Parameters)] = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    val folders = new File("out/parameterTuner/random").listFiles() ++
      new File("out/parameterTuner/random-cauldron").listFiles()

    val shuffledFolders = Random.shuffle(folders.toSeq)

    val results = shuffledFolders.flatMap { folder =>
      Try {
        val paramsFile = new File(s"${folder.getAbsolutePath}/params.spkl")
        val oos = new ObjectInputStream(new FileInputStream(paramsFile))
        val params = oos.readObject().asInstanceOf[Parameters]
        oos.close()

        val resultsFile = new File(s"${folder.getAbsolutePath}/final_results.json")
        val results = mapper.readValue(
          resultsFile,
          new TypeReference[Map[String, Map[String, Double]]] {}
        )

        (results, params)
      }.toOption
    }

    logger.info(s"Got results for ${results.length} tests from ${folders.length} folders")

    results
  }

  /** Creates a SMAC PCS file that specifies the bounds and restrictions of
    * available parameters.
    */
  def populateSmacParameterSpec(
    parameterSpecFile: String,
    detectors        : DetectorType.ValueBuilder*
  ): Unit = {
    import nz.net.wand.streamevmon.tuner.ParameterSpecToSmac._

    // We only write the parameters for detectors we'll be using
    val allParameterSpecs = detectors.flatMap(DetectorParameterSpecs.parametersFromDetectorType)
    // We handle fixed parameters as single-field categorical variables to
    // ensure they don't have other values generated.
    val fixedParameters = DetectorParameterSpecs.fixedParameters

    FileUtils.forceMkdir(new File(parameterSpecFile).getParentFile)
    val writer = new BufferedWriter(new FileWriter(parameterSpecFile))

    // First we write down the simple specifications of parameter bounds.
    // toSmacString() takes an optional fixed parameter specification.
    allParameterSpecs.foreach { spec =>
      writer.write(spec.toSmacString(fixedParameters.get(spec.name)))
      writer.newLine()
    }

    // Throw a newline in so it's a little more readable.
    writer.newLine()

    // Let's now get the parameter restrictions for the detectors we're using.
    // Not all detectors even have restrictions, so this could well end out
    // being an empty list.
    // toSmacString() handles all the heavy lifting.
    val restrictions = detectors.flatMap(DetectorParameterSpecs.parameterRestrictionsFromDetectorType)
    restrictions.foreach { rest =>
      writer.write(rest.toSmacString)
      writer.newLine()
    }

    // Flush it to make sure it's all written properly.
    writer.flush()
    writer.close()
  }

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
    populateSmacParameterSpec(parameterSpecFile, detectorsToUse: _*)

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
