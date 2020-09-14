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

import scala.concurrent.{Await, Future, TimeoutException}
import scala.concurrent.duration.{Duration, MINUTES}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.sys.process._

case class NabJob(
  params   : Parameters,
  outputDir: String,
  detectors: Iterable[DetectorType.ValueBuilder] = NabJob.allDetectors
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

      logger.info(s"Starting detectors...")
      logger.debug(s"Using parameters: ")
      params.getAsArgs.grouped(2).foreach {
        arg => logger.debug(arg.mkString(" "))
      }
      val runner = new NabAllDetectors(detectors)
      runner.runOnAllNabFiles(params.getAsArgs.toArray, outputDir, deleteOutputDirectory = false)

      val timeAfterDetectors = System.currentTimeMillis()

      logger.info("Adding reference folder for scorer...")
      FileUtils.copyDirectory(new File("data/NAB/results/null"), new File(s"$outputDir/null"))

      logger.info(s"Scoring tests from job $this...")

      val errBuf = new FileProcessLogger(new File(s"$outputDir/scorer.error.log"))
      val outBuf = new FileProcessLogger(new File(s"$outputDir/scorer.log"))

      def flushAndClose(): Unit = {
        outBuf.flush()
        errBuf.flush()
        outBuf.close()
        errBuf.close()
      }

      try {
        Await.result(Future(
          try {
            val output = Seq(
              "./scripts/nab/nab-scorer.sh",
              detectors.mkString(","),
              FilenameUtils.normalize(new File(outputDir).getAbsolutePath),
              System.getProperty("nz.net.wand.streamevmon.tuner.pythonProfileParameter")
            ).lineStream(errBuf)

            output.foreach { line =>
              outBuf.out(line)
              outBuf.flush()
            }
          }
          catch {
            case e: Exception =>
              if (new File(s"$outputDir/final_results.json").exists()) {
                logger.info("Scorer exited with non-zero return code, but it appeared to have finished. Continuing.")
              }
              else {
                throw new RuntimeException(s"NAB scorer exited with non-zero return code. Check $outputDir/scorer.log for details.", e)
              }
          }
          // We managed to get the scorer to take up to 40 seconds, so a minute should be fine here.
          // It usually exits just fine, but sometimes it decides to hang after the output file has been created.
        ), Duration(1, MINUTES))
      }
      catch {
        case e: TimeoutException =>
          flushAndClose()
          new File(s"$outputDir/TIMEOUT").createNewFile()
          if (new File(s"$outputDir/final_results.json").exists()) {
            logger.info("Scorer timed out, but it appeared to have finished. Continuing.")
          }
          else {
            logger.error("Scorer timed out.")
            throw e
          }
        case e: Exception =>
          flushAndClose()
          throw e
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
      val runtimeWriter = new BufferedWriter(new FileWriter(s"$outputDir/runtime.log"))
      runtimeWriter.write(s"${endTime - startTime}")
      runtimeWriter.newLine()
      runtimeWriter.flush()
      runtimeWriter.close()

      if (System.getProperty("nz.net.wand.streamevmon.tuner.cleanupNabOutputs").toBoolean) {
        logger.info("Tidying up output folder...")
        (DetectorType.values.map(det => s"$outputDir/${det.toString}") ++ Seq(s"$outputDir/null"))
          .foreach { folder =>
            FileUtils.deleteQuietly(new File(folder))
          }
      }
      else {
        logger.info("Not tidying output folder. Beware of disk space!")
      }

      Runtime.getRuntime.removeShutdownHook(shutdownHookThread)

      getResult(results, timeAfterDetectors - timeBeforeDetectors, endTime - startTime)
    }
    catch {
      case e: Exception =>
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
