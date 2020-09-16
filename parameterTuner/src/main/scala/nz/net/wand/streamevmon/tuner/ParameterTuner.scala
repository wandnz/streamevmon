package nz.net.wand.streamevmon.tuner

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.detectors.changepoint.ChangepointDetector
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

object ParameterTuner extends Logging {

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

  val baseOutputDir = "out/parameterTuner"
  val smacdir = "smac"

  def populateSmacParameterSpec(
    parameterSpecFile: String,
    detectors        : DetectorType.ValueBuilder*
  ): Unit = {
    import nz.net.wand.streamevmon.tuner.ParameterSpecToSmac._

    val allParameterSpecs = detectors.flatMap(DetectorParameterSpecs.parametersFromDetectorType)
    val fixedParameters = DetectorParameterSpecs.fixedParameters

    FileUtils.forceMkdir(new File(parameterSpecFile).getParentFile)
    val writer = new BufferedWriter(new FileWriter(parameterSpecFile))

    allParameterSpecs.foreach { spec =>
      // Note that this makes no use of forbidden parameter clauses, so some
      // generated values may violate DetectorParameterSpec.parametersAreValid.
      writer.write(spec.toSmacString(fixedParameters.get(spec.name)))
      writer.newLine()
    }

    writer.newLine()

    val restrictions = ChangepointDetector.parameterRestrictions
    restrictions.foreach { rest =>
      writer.write(rest.toSmacString)
      writer.newLine()
    }

    writer.flush()
    writer.close()
  }

  def main(args: Array[String]): Unit = {
    // Squash all the logs from Flink to tidy up our output.
    System.setProperty("org.slf4j.simpleLogger.log.org.apache.flink", "error")
    System.setProperty(
      "org.slf4j.simpleLogger.log.nz.net.wand.streamevmon.tuner.nab.NabAllDetectors",
      "error"
    )
    //System.setProperty("org.slf4j.simpleLogger.log.nz.net.wand.streamevmon.tuner.nab.smac", "info")
    System.setProperty("org.slf4j.simpleLogger.showDateTime", "true")
    System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "HH:mm:ss")

    val opts = ProgramOptions(args)
    val detectorsToUse = opts.getDetectors
    val scoreTargets = opts.getScoreTargets

    if (detectorsToUse.isEmpty) {
      throw new IllegalArgumentException("Can't start without any detectors")
    }
    if (scoreTargets.isEmpty) {
      throw new IllegalArgumentException("Can't start without any score targets")
    }

    val rungroup =
      s"${detectorsToUse.mkString(",")}-" +
        s"${scoreTargets.mkString(",")}-" +
        s"${Instant.ofEpochMilli(System.currentTimeMillis())}"

    val parameterSpecFile = s"$baseOutputDir/$smacdir/$rungroup/parameterspec.smac"

    System.setProperty("nz.net.wand.streamevmon.tuner.detectors", detectorsToUse.mkString(","))
    System.setProperty("nz.net.wand.streamevmon.tuner.scoreTargets", scoreTargets.mkString(","))
    System.setProperty("nz.net.wand.streamevmon.tuner.runOutputDir", s"$baseOutputDir/$smacdir/$rungroup/run-outputs")
    System.setProperty("nz.net.wand.streamevmon.tuner.pythonProfileParameter", "noProfile") // Set to "profile" if you want that behaviour
    System.setProperty("nz.net.wand.streamevmon.tuner.cleanupNabOutputs", "true")

    populateSmacParameterSpec(parameterSpecFile, detectorsToUse: _*)

    System.setOut(new PrintStream(new TeeOutputStream(new FileOutputStream(s"$baseOutputDir/$smacdir/$rungroup/stdout.log"), new FileOutputStream(FileDescriptor.out))))

    logger.info(s"Using detectors ${detectorsToUse.mkString("(", ", ", ")")} and score targets ${scoreTargets.mkString("(", ", ", ")")}")

    SMACExecutor.oldMain(
      Array(
        "--tae", new NabTAEFactory().getName,
        "--algo-exec", "",
        "--run-obj", "QUALITY",
        "--pcs-file", parameterSpecFile,
        "--use-instances", "false",
        "--experiment-dir", baseOutputDir,
        "--output-dir", smacdir,
        "--rungroup", rungroup,
        "--cputime-limit", opts.cputimeLimit.toString,
        "--iteration-limit", opts.iterationLimit.toString,
        "--wallclock-limit", opts.wallclockLimit.toString,
        "--runcount-limit", opts.runcountLimit.toString,
        "--doValidation", opts.doValidation.toString
      ))

    logger.info("SMAC finished. Shutting down ActorSystem.")
    ConfiguredPipelineRunner.shutdownImmediately()
  }
}
