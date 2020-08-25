package nz.net.wand.streamevmon.runners.tuner.nab

import nz.net.wand.streamevmon.runners.tuner.jobs.{Job, JobResult}
import nz.net.wand.streamevmon.runners.unified.schema.DetectorType

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
  detectors: Iterable[DetectorType.ValueBuilder] = Seq(
    DetectorType.Baseline,
    DetectorType.Changepoint,
    DetectorType.DistDiff,
    DetectorType.Mode,
    DetectorType.Spike
  ),
  skipDetectors: Boolean = false,
  skipScoring  : Boolean = false
) extends Job(uid) {
  override def run(): JobResult = {

    if (!skipDetectors) {
      logger.info(s"Starting detectors...")
      val runner = new NabAllDetectors(detectors)
      runner.runOnAllNabFiles(args, outputDir)
    }
    else {
      logger.info(s"Skipping detectors...")
    }

    if (!skipScoring) {
      logger.info("Making output directory sane for scorer...")
      FileUtils.copyDirectory(new File("data/NAB/results/null"), new File(s"$outputDir/null"))

      logger.info(s"Scoring tests from job $this...")
      Seq(
        "./scripts/nab/nab-scorer.sh",
        detectors.mkString(","),
        FilenameUtils.normalize(new File(outputDir).getAbsolutePath)
      ).!!
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
