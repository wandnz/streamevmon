package nz.net.wand.streamevmon.tuner

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.parameters.{DetectorParameterSpecs, Parameters}
import nz.net.wand.streamevmon.runners.unified.schema.DetectorType
import nz.net.wand.streamevmon.tuner.nab.ScoreTarget
import nz.net.wand.streamevmon.tuner.nab.smac.NabTAEFactory

import java.io._

import ca.ubc.cs.beta.smac.executors.SMACExecutor
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

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

  val parameterSpecFile = "out/parameterTuner/smac/parameterspec.smac"

  def populateSmacParameterSpec(detectors: DetectorType.ValueBuilder*): Unit = {
    val allParameterSpecs = detectors.flatMap(DetectorParameterSpecs.parametersFromDetectorType)
    val fixedParameters = DetectorParameterSpecs.fixedParameters

    val writer = new BufferedWriter(new FileWriter(parameterSpecFile))

    allParameterSpecs.foreach { spec =>
      // Note that this makes no use of forbidden parameter clauses, so some
      // generated values may violate DetectorParameterSpec.parametersAreValid.
      writer.write(spec.toSmacString(fixedParameters.get(spec.name)))
      writer.newLine()
    }
    writer.flush()
    writer.close()
  }

  def parseArgs(args: Array[String]): Unit = {
    detectorsToUse = args.flatMap {
      arg => Try(DetectorType.withName(arg)).toOption.asInstanceOf[Option[DetectorType.ValueBuilder]]
    }.toSeq
    logger.info(s"Using detectors ${detectorsToUse.mkString("(", ", ", ")")} and score targets ${scoreTargets.mkString("(", ", ", ")")}")
  }

  def main(args: Array[String]): Unit = {
    // Squash all the logs from Flink to tidy up our output.
    System.setProperty("org.slf4j.simpleLogger.log.org.apache.flink", "error")
    System.setProperty(
      "org.slf4j.simpleLogger.log.nz.net.wand.streamevmon.runners.tuner.nab.NabAllDetectors",
      "error"
    )

    parseArgs(args)

    if (detectorsToUse.isEmpty) {
      throw new IllegalArgumentException("Can't start without any detectors")
    }
    if (scoreTargets.isEmpty) {
      throw new IllegalArgumentException("Can't start without any score targets")
    }

    populateSmacParameterSpec(detectorsToUse: _*)

    SMACExecutor.oldMain(
      Array(
        "--tae", new NabTAEFactory().getName,
        "--algo-exec", "",
        "--run-obj", "QUALITY",
        "--pcs-file", parameterSpecFile,
        "--use-instances", "false",
        "--experiment-dir", "out/parameterTuner/smac",
        "--output-dir", "smac-output"
      ))
  }

  var detectorsToUse: Seq[DetectorType.ValueBuilder] = Seq(DetectorType.Baseline)
  var scoreTargets: Iterable[ScoreTarget.Value] = Seq(ScoreTarget.Standard)
}
