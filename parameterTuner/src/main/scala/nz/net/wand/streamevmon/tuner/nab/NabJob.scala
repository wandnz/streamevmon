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

/** This runs the specified detectors against the NAB benchmark with the given
  * parameters, and returns a [[NabJobResult]] containing the scores. run()
  * could take a while to execute.
  */
case class NabJob(
  params   : Parameters,
  outputDir: String,
  detectors: Iterable[DetectorType.ValueBuilder]
) extends Job(params.hashCode.toString) {

  override val toString: String = s"NabJob-${params.hashCode.toString}"

  /** If the JVM shuts down while we're still running, we'll make sure we log
    * that that happened.
    */
  private val shutdownHookThread: Thread = new Thread() {
    override def run(): Unit = {
      logger.error("Job interrupted.")
      new File(s"$outputDir/INTERRUPTED").createNewFile()
    }
  }

  /** Subclasses may override this function to provide a more detailed result.
    */
  protected def getResult(
    results      : Map[String, Map[String, Double]],
    runtime      : Double,
    wallClockTime: Double
  ): NabJobResult = {
    NabJobResult(this, results)
  }

  /** Subclasses may override this function to provide a more detailed failure
    * result.
    */
  protected def onFailure(
    e            : Exception,
    runtime      : Double,
    wallClockTime: Double
  ): JobResult = {
    FailedJob(this, e)
  }

  /** Runs detectors against NAB, scores it, and leaves some results behind in
    * the configured output folder.
    */
  override def run(): JobResult = {
    // SMAC likes to know about the runtime for everything. We don't count the
    // scorer runtime as part of the algorithm, but we do count it for the
    // wallclock time passed.
    var startTime = -1L
    var endTime = -1L
    var timeBeforeDetectors = -1L
    var timeAfterDetectors = -1L

    // Any exceptions must be handled gracefully.
    try {
      // This will be executed if we don't get a chance to remove it before the
      // JVM exits.
      Runtime.getRuntime.addShutdownHook(shutdownHookThread)

      startTime = System.currentTimeMillis()

      // We'll write down the parameters in a new directory. When the job
      // finishes, we'll also write the results in this directory.
      Files.createDirectories(Paths.get(outputDir))
      val parameterWriter = new BufferedWriter(new FileWriter(s"$outputDir/params.properties"))
      // They're written in the .properties format, but that's just for human readability.
      params.elems.sortBy(_.name).foreach { p =>
        parameterWriter.write(s"${p.name}=${p.value}")
        parameterWriter.newLine()
      }
      parameterWriter.flush()
      parameterWriter.close()

      // We'll also serialize the Parameters object so we don't have to bother
      // parsing a properties file if we ever want to load these parameters again.
      val oos = new ObjectOutputStream(new FileOutputStream(s"$outputDir/params.spkl"))
      oos.writeObject(params)
      oos.close()

      timeBeforeDetectors = System.currentTimeMillis()

      logger.info(s"Starting detectors...")
      logger.debug(s"Using parameters: ")
      params.getAsArgs.grouped(2).foreach {
        arg => logger.debug(arg.mkString(" "))
      }

      // Launch the detectors. This takes a while, but we set no timeout because
      // we need it to finish.
      val runner = new NabAllDetectors(detectors)
      runner.runOnAllNabFiles(params.getAsArgs.toArray, outputDir, deleteOutputDirectoryBeforeStart = false)

      timeAfterDetectors = System.currentTimeMillis()

      // NAB scorer can't execute without the reference folder containing null results.
      logger.info("Adding reference folder for scorer...")
      FileUtils.copyDirectory(new File("data/NAB/results/null"), new File(s"$outputDir/null"))

      logger.info(s"Scoring tests from job $this...")

      // We want to keep track of all output from the scorer,
      val errBuf = new FileProcessLogger(new File(s"$outputDir/scorer.error.log"))
      val outBuf = new FileProcessLogger(new File(s"$outputDir/scorer.log"))

      // and we want to make sure it all makes it to the disk.
      def flushAndClose(): Unit = {
        outBuf.flush()
        errBuf.flush()
        outBuf.close()
        errBuf.close()
      }

      // One layer of exception handling for if the scorer runs for longer than our custom timeout
      try {
        Await.result(Future(
          // Another layer for if it exits with a non-zero return code
          try {
            // This calls the scorer as a subprocess. We use a wrapper script to
            // keep execution consistent with if we did it manually.
            val output = Seq(
              "./scripts/nab/nab-scorer.sh",
              // It needs to know the detectors to work on,
              detectors.mkString(","),
              // where their results ended up,
              FilenameUtils.normalize(new File(outputDir).getAbsolutePath),
              // and whether to run a profiler on the scorer.
              System.getProperty("nz.net.wand.streamevmon.tuner.pythonProfileParameter")
            ).lineStream(errBuf)

            output.foreach { line =>
              outBuf.out(line)
              outBuf.flush()
            }
          }
            // It's possible that the scorer exits with a non-zero return code, but the results are still present.
            // We should treat that as a success.
          catch {
            case e: Exception =>
              new File(s"$outputDir/SCORER_EXIT_CODE").createNewFile()
              if (new File(s"$outputDir/final_results.json").exists()) {
                logger.info("Scorer exited with non-zero return code, but it appeared to have finished. Continuing.")
              }
              else {
                // This exception will bubble up as a run failure.
                throw new RuntimeException(s"NAB scorer exited with non-zero return code. Check $outputDir/scorer.log for details.", e)
              }
          }
          // We managed to get the scorer to take up to 40 seconds, so a minute should be fine here.
          // It usually exits just fine, but sometimes it decides to hang after the output file has been created.
        ), Duration(1, MINUTES))
      }
      catch {
        // Same case as the non-zero return code here - it might have actually finished, which we should treat happily.
        case e: TimeoutException =>
          flushAndClose()
          new File(s"$outputDir/SCORER_TIMEOUT").createNewFile()
          if (new File(s"$outputDir/final_results.json").exists()) {
            logger.info("Scorer timed out, but it appeared to have finished. Continuing.")
          }
          else {
            logger.error("Scorer timed out.")
            endTime = System.currentTimeMillis()
            // This will bubble up as a failure.
            throw e
          }
        case e: Exception =>
          // Any other exception we don't recognise - including the one from the non-zero return code - will bubble up
          // as a failure.
          flushAndClose()
          endTime = System.currentTimeMillis()
          throw e
      }

      // If we reach this point, we got a result from the scorer, so we should read it in.
      // It's got a consistent JSON schema, so we get a Map.
      logger.info("Parsing results...")
      val mapper = new ObjectMapper()
      mapper.registerModule(DefaultScalaModule)
      val results: Map[String, Map[String, Double]] = mapper.readValue(
        new File(s"$outputDir/final_results.json"),
        new TypeReference[Map[String, Map[String, Double]]] {}
      )

      endTime = System.currentTimeMillis()

      logger.info(s"Scores: $results")

      // Write down the runtime for future reference.
      logger.info(s"Algorithm time taken: ${timeAfterDetectors - timeBeforeDetectors}ms")
      logger.info(s"Wallclock time taken: ${endTime - startTime}ms")
      val runtimeWriter = new BufferedWriter(new FileWriter(s"$outputDir/runtime.log"))
      runtimeWriter.write(s"${endTime - startTime}")
      runtimeWriter.newLine()
      runtimeWriter.flush()
      runtimeWriter.close()

      // If we need to tidy up to save disk space, do so. These outputs can grow very quickly.
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

      // We made it to the end, so we don't need this notice anymore.
      Runtime.getRuntime.removeShutdownHook(shutdownHookThread)

      getResult(results, timeAfterDetectors - timeBeforeDetectors, endTime - startTime)
    }
    catch {
      case e: Exception =>
        // This could be a number of different errors, but we treat them all the same way.
        val writer = new PrintWriter(new File(s"$outputDir/fail.log"))
        e.printStackTrace(writer)
        writer.flush()
        writer.close()
        // We didn't get killed, so we don't need this anymore.
        Runtime.getRuntime.removeShutdownHook(shutdownHookThread)
        onFailure(e, Math.max(timeAfterDetectors - timeBeforeDetectors, 0), Math.max(endTime - startTime, 0))
    }
  }
}
