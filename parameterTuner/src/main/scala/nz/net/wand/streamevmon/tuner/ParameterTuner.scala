package nz.net.wand.streamevmon.tuner

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.parameters.Parameters
import nz.net.wand.streamevmon.tuner.nab.smac.NabTAEFactory

import java.io.{File, FileInputStream, ObjectInputStream}

import ca.ubc.cs.beta.smac.executors.SMACExecutor
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.util.{Random, Try}

object ParameterTuner extends Logging {

  def main(args: Array[String]): Unit = {
    // Squash all the logs from Flink to tidy up our output.
    System.setProperty("org.slf4j.simpleLogger.log.org.apache.flink", "error")
    System.setProperty(
      "org.slf4j.simpleLogger.log.nz.net.wand.streamevmon.runners.tuner.nab.NabAllDetectors",
      "error")

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

    SMACExecutor.oldMain(
      Array(
        "--tae", new NabTAEFactory().getName,
        "--algo-exec", "",
        "--run-obj", "QUALITY",
        "--pcs-file", "out/parameterspec.smac",
        "--use-instances", "false",
        //"--scenario-file", "out/smac.scenario"
      ))
  }
}
