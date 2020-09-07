package nz.net.wand.streamevmon.tuner.nab

import nz.net.wand.streamevmon.parameters.Parameters
import nz.net.wand.streamevmon.runners.unified.schema.DetectorType
import nz.net.wand.streamevmon.tuner.jobs.{FailedJob, Job, JobResult}

import java.io._
import java.nio.file.{Files, Paths}

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.commons.io.{FilenameUtils, FileUtils}

import scala.sys.process._

case class NabJob(
  params: Parameters,
  outputDir    : String,
  detectors    : Iterable[DetectorType.ValueBuilder] = NabJob.allDetectors,
  skipDetectors: Boolean = false,
  skipScoring  : Boolean = false
) extends Job(params.hashCode.toString) {

  override val toString: String = s"NabJob-${params.hashCode.toString}"

  private val shutdownHookThread: Thread = new Thread() {
    override def run(): Unit = {
      logger.error("Job interrupted.")
      new File(s"$outputDir/INTERRUPTED").createNewFile()
    }
  }

  protected def getResult(
    results: Map[String, Map[String, Double]],
    runtime: Double,
    wallClockTime: Double
  ): NabJobResult = {
    NabJobResult(this, results)
  }

  override def run(): JobResult = {
    try {
      Runtime.getRuntime.addShutdownHook(shutdownHookThread)

      val startTime = System.currentTimeMillis()

      // We'll write down the parameters in a new directory. When the job
      // finishes, we'll also write the results in this directory.
      Files.createDirectories(Paths.get(outputDir))
      val parameterWriter = new BufferedWriter(new FileWriter(s"$outputDir/params.properties"))
      params.elems.sortBy(_.name).foreach {
        p =>
          parameterWriter.write(s"${p.name}=${p.value}")
          parameterWriter.newLine()
      }
      parameterWriter.flush()
      parameterWriter.close()

      // We'll also serialize the Parameters object so we don't have to bother
      // parsing a properties file later.
      val oos = new ObjectOutputStream(new FileOutputStream(s"$outputDir/params.spkl"))
      oos.writeObject(params)
      oos.close()

      val timeBeforeDetectors = System.currentTimeMillis()

      if (!skipDetectors) {
        logger.info(s"Starting detectors...")
        logger.debug(s"Using parameters: ")
        params.getAsArgs.grouped(2).foreach {
          arg => logger.debug(arg.mkString(" "))
        }
        val runner = new NabAllDetectors(detectors)
        runner.runOnAllNabFiles(params.getAsArgs.toArray, outputDir, deleteOutputDirectory = false)
      }
      else {
        logger.info(s"Skipping detectors...")
      }

      val timeAfterDetectors = System.currentTimeMillis()

      if (!skipScoring) {
        logger.info("Making output directory sane for scorer...")
        FileUtils.copyDirectory(new File("data/NAB/results/null"), new File(s"$outputDir/null"))

        logger.info(s"Scoring tests from job $this...")
        val output = Seq(
          "./scripts/nab/nab-scorer.sh",
          detectors.mkString(","),
          FilenameUtils.normalize(new File(outputDir).getAbsolutePath)
        ).!!

        val writer = new BufferedWriter(new FileWriter(s"$outputDir/scorer.log"))
        writer.write(output)
        writer.newLine()
        writer.flush()
        writer.close()
      }
      else {
        logger.info("Skipping scoring...")
      }

      logger.info("Parsing results...")
      val mapper = new ObjectMapper()
      mapper.registerModule(DefaultScalaModule)
      val results: Map[String, Map[String, Double]] = mapper.readValue(
        new File(s"$outputDir/final_results.json"),
        new TypeReference[Map[String, Map[String, Double]]] {}
      )

      val endTime = System.currentTimeMillis()

      logger.info(s"Scores: $results")

      logger.info(s"Algorithm time taken: ${timeAfterDetectors - timeBeforeDetectors}ms")
      logger.info(s"Wallclock time taken: ${endTime - startTime}ms")
      val writer = new BufferedWriter(new FileWriter(s"$outputDir/runtime.log"))
      writer.write(s"${endTime - startTime}")
      writer.newLine()
      writer.flush()
      writer.close()

      Runtime.getRuntime.removeShutdownHook(shutdownHookThread)

      getResult(results, timeAfterDetectors - timeBeforeDetectors, endTime - startTime)
    }
    catch {
      case e: RuntimeException =>
        val writer = new PrintWriter(new File(s"$outputDir/fail.log"))
        e.printStackTrace(writer)
        writer.flush()
        writer.close()
        FailedJob(this, e)
    }
  }
}

object NabJob {
  val allDetectors = Seq(
    DetectorType.Baseline,
    DetectorType.Changepoint,
    DetectorType.DistDiff,
    DetectorType.Mode,
    DetectorType.Spike
  )
}
