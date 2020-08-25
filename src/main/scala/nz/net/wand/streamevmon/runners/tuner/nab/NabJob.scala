package nz.net.wand.streamevmon.runners.tuner.nab

import nz.net.wand.streamevmon.runners.tuner.jobs.{Job, JobResult}

import java.io.File

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.commons.io.{FilenameUtils, FileUtils}

import scala.sys.process._

case class NabJob(
  uid: String,
  args: Array[String],
  outputDir: String,
  skipDetectors: Boolean = false,
  skipScoring: Boolean = false
) extends Job(uid) {
  override def run(): JobResult = {

    if (!skipDetectors) {
      logger.info(s"Starting tests...")
      val runner = new NabAllDetectors
      runner.runOnAllNabFiles(args, outputDir)
    }
    else {
      logger.info(s"Skipping tests...")
    }

    logger.info("Making output directory sane for scorer...")
    FileUtils.copyDirectory(new File("data/NAB/results/null"), new File(s"$outputDir/null"))

    if (!skipScoring) {
      logger.info(s"Scoring tests from job $this...")
      Seq("./scripts/nab/nab-scorer.sh", FilenameUtils.normalize(new File(outputDir).getAbsolutePath)).!!
    }
    else {
      logger.info("Skipping scoring...")
    }

    logger.info("Parsing results...")
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    val results = mapper.readValue(
      new File(s"$outputDir/final_results.json"),
      new TypeReference[Map[String, Map[String, String]]] {}
    )

    NabJobResult(this, results)
  }
}
