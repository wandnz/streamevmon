package nz.net.wand.streamevmon.tuner.nab.smac

import nz.net.wand.streamevmon.parameters.{DetectorParameterSpecs, ParameterInstance, Parameters}
import nz.net.wand.streamevmon.runners.unified.schema.DetectorType
import nz.net.wand.streamevmon.tuner.nab.{NabJob, ScoreTarget}

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus

import scala.collection.JavaConverters._

class SmacNabJob(
  runConfig: AlgorithmRunConfiguration,
  params: Parameters,
  detectors: Iterable[DetectorType.ValueBuilder],
  optimiseFor: Iterable[ScoreTarget.Value],
  skipDetectors: Boolean = false,
  skipScoring: Boolean = false
) extends NabJob(
  params,
  s"./out/parameterTuner/smac/${params.hashCode.toString}",
  detectors,
  skipDetectors,
  skipScoring
) {
  override protected def getResult(
    results: Map[String, Map[String, Double]],
    runtime      : Double,
    wallClockTime: Double
  ): SmacNabJobResult = {

    val scores = detectors.flatMap { det =>
      optimiseFor.map { target =>
        results(det.toString)(target.toString)
      }
    }

    if (scores.isEmpty) {
      throw new UnsupportedOperationException("Can't get score for zero results!")
    }

    val score = scores.sum / scores.size
    val quality = 100.0 - score
    new SmacNabJobResult(this, results, NabAlgorithmRunResult(
      runConfig,
      RunStatus.SAT,
      runtime,
      quality,
      outputDir,
      wallClockTime
    ))
  }

  override val toString: String = s"SmacNabJob-${params.hashCode.toString}"
}

object SmacNabJob {
  def apply(
    runConfig: AlgorithmRunConfiguration,
    detectors    : Iterable[DetectorType.ValueBuilder],
    optimiseFor  : Iterable[ScoreTarget.Value],
    skipDetectors: Boolean = false,
    skipScoring  : Boolean = false
  ): SmacNabJob = {

    val params = runConfig.getParameterConfiguration

    val paramSpecs = DetectorParameterSpecs.getAllDetectorParameters

    val paramsWithSpecs = params.asScala.map {
      case (k, v) =>
        paramSpecs.find(_.name == k).map { spec =>
          new ParameterInstance[Any](spec, v)
        } match {
          case Some(value) => value
          case None => throw new NoSuchElementException(s"Could not find ParameterSpec for $k!")
        }
    }

    val parameters = new Parameters(paramsWithSpecs.toSeq: _*)

    new SmacNabJob(
      runConfig,
      parameters,
      detectors,
      optimiseFor,
      skipDetectors,
      skipScoring
    )
  }
}
