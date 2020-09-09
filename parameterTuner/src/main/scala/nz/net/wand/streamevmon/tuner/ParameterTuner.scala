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

  val parameterSpecFile = "out/parameterTuner/parameterspec.smac"

  def populateSmacParameterSpec(detectors: DetectorType.ValueBuilder*): Unit = {
    val allParameterSpecs = detectors.flatten(DetectorParameterSpecs.parametersFromDetectorType)
    val fixedParameters = DetectorParameterSpecs.fixedParameters

    val writer = new BufferedWriter(new FileWriter(parameterSpecFile))

    allParameterSpecs.foreach { spec =>
      writer.write(spec.toSmacString(fixedParameters.get(spec.name)))
      writer.newLine()
    }
    writer.flush()
    writer.close()
  }

  def main(args: Array[String]): Unit = {
    // Squash all the logs from Flink to tidy up our output.
    System.setProperty("org.slf4j.simpleLogger.log.org.apache.flink", "error")
    System.setProperty(
      "org.slf4j.simpleLogger.log.nz.net.wand.streamevmon.runners.tuner.nab.NabAllDetectors",
      "error"
    )

    populateSmacParameterSpec(DetectorType.Baseline)

    SMACExecutor.oldMain(
      Array(
        "--tae", new NabTAEFactory().getName,
        "--algo-exec", "",
        "--run-obj", "QUALITY",
        "--pcs-file", parameterSpecFile,
        "--use-instances", "false",
        //"--scenario-file", "out/smac.scenario"
      ))
  }

  var detectorsToUse: Seq[DetectorType.ValueBuilder] = Seq(DetectorType.Baseline)
  var scoreTarget: ScoreTarget.Value = ScoreTarget.Standard
}
