package nz.net.wand.streamevmon.tuner.nab.smac

import nz.net.wand.streamevmon.tuner.ParameterTuner

import java.util

import ca.ubc.cs.beta.aeatk.options.AbstractOptions
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.{TargetAlgorithmEvaluator, TargetAlgorithmEvaluatorFactory}

class NabTAEFactory extends TargetAlgorithmEvaluatorFactory {

  private lazy val detectors = ParameterTuner.detectorsToUse
  private lazy val scoreTargets = ParameterTuner.scoreTargets
  private lazy val baseOutputDir = ParameterTuner.runOutputDir

  override def getName: String = "NAB"

  override def getTargetAlgorithmEvaluator: TargetAlgorithmEvaluator = new NabTAE(detectors, scoreTargets, baseOutputDir)

  override def getTargetAlgorithmEvaluator(options: AbstractOptions): TargetAlgorithmEvaluator = new NabTAE(detectors, scoreTargets, baseOutputDir)

  override def getTargetAlgorithmEvaluator(optionsMap: util.Map[String, AbstractOptions]): TargetAlgorithmEvaluator = new NabTAE(detectors, scoreTargets, baseOutputDir)

  override def getOptionObject: AbstractOptions = new AbstractOptions {}
}
