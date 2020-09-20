package nz.net.wand.streamevmon.tuner

import nz.net.wand.streamevmon.runners.unified.schema.DetectorType
import nz.net.wand.streamevmon.tuner.nab.ScoreTarget
import nz.net.wand.streamevmon.Logging

import java.util

import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.{FixedPositiveInteger, FixedPositiveLong, NonNegativeInteger}
import com.beust.jcommander.{JCommander, Parameter}

import scala.collection.JavaConverters._
import scala.util.{Failure, Try}

class ProgramOptions {
  @Parameter(
    names = Array("--help", "-h"),
    description = "Show this help message"
  )
  val help: Boolean = false

  @Parameter(
    names = Array("--detectors", "-d"),
    description = "Comma or space separated list of valid detector names",
    variableArity = true,
    required = true
  )
  private val detectors: util.List[String] = new util.ArrayList()

  def getDetectors: Seq[DetectorType.ValueBuilder] = detectors.asScala.map(
    d => DetectorType.withName(d).asInstanceOf[DetectorType.ValueBuilder]
  )

  @Parameter(
    names = Array("--scoreTargets", "-s"),
    description = "Comma or space separated list of valid score target names",
    variableArity = true
  )
  private val scoreTargets: util.List[String] = Seq("standard").asJava

  def getScoreTargets: Seq[ScoreTarget.Value] = scoreTargets.asScala.map(
    s => ScoreTarget.withName(s)
  )

  @Parameter(
    names = Array("--doValidation"),
    description = "If true, SMAC will perform a validation run on the best-scoring parameter set when finishing up",
    arity = 1
  )
  val doValidation: Boolean = true

  @Parameter(
    names = Array("--doPythonProfiling"),
    description = "If true, the NAB scorer will be profiled by py-spy. You can upload the resulting scorer-speedscope.json file to https://speedscope.app to view the flamegraph.",
    arity = 1
  )
  private val doPythonProfiling: Boolean = false
  lazy val doPythonProfilingKeyword: String = if (doPythonProfiling) {
    "profile"
  }
  else {
    "no-profile"
  }

  @Parameter(
    names = Array("--cleanupNabScorerOutputs"),
    description = "If true, the outputs produced by the NAB scorer will be tidied after every algorithm run. This is NOT destructive of results, but it IS destructive of the raw outputs from the algorithm run.",
    arity = 1
  )
  val cleanupNabScorerOutputs: Boolean = true

  @Parameter(
    names = Array("--cputime-limit"),
    description = "limits the total cpu time allowed between SMAC and the target algorithm runs during the automatic configuration phase",
    validateWith = classOf[NonNegativeInteger]
  )
  val cputimeLimit: Int = Int.MaxValue

  @Parameter(
    names = Array("--retry-crashed-count"),
    description = "number of times to retry an algorithm before reporting it crashed if it returns the CRASHED status",
    validateWith = classOf[FixedPositiveInteger]
  )
  val retryCrashedCount: Int = 2

  @Parameter(
    names = Array("--iteration-limit"),
    description = "limits the number of iterations allowed during automatic configuration phase",
    validateWith = classOf[FixedPositiveInteger]
  )
  val iterationLimit: Int = Int.MaxValue

  @Parameter(
    names = Array("--wallclock-limit"),
    description = "limits the total wall-clock time allowed during the automatic configuration phase",
    validateWith = classOf[FixedPositiveInteger]
  )
  val wallclockLimit: Int = Int.MaxValue

  @Parameter(
    names = Array("--runcount-limit"),
    description = "limits the total number of target algorithm runs allowed during the automatic configuration phase",
    validateWith = classOf[FixedPositiveLong]
  )
  val runcountLimit: Long = Long.MaxValue
}

object ProgramOptions extends Logging {
  def apply(args: Array[String]): ProgramOptions = {
    val opts = new ProgramOptions
    val jcom = new JCommander(opts, false, false)

    Try(jcom.parse(args: _*)) match {
      case _ if opts.help =>
        println(jcom.usage())
        System.exit(1)
      case Failure(exception) =>
        println(exception.getMessage)
        println(jcom.usage())
        System.exit(1)
      case _ =>
    }

    opts
  }
}
