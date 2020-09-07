package nz.net.wand.streamevmon.tuner.nab

import nz.net.wand.streamevmon.runners.unified.schema.DetectorType

import java.util

import ca.ubc.cs.beta.aeatk.options.AbstractOptions
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.{TargetAlgorithmEvaluator, TargetAlgorithmEvaluatorFactory}

class NabTAEFactory extends TargetAlgorithmEvaluatorFactory {

  private val detector = DetectorType.Baseline
  private val scoreTarget = ScoreTarget.Standard

  override def getName: String = "NAB"

  override def getTargetAlgorithmEvaluator: TargetAlgorithmEvaluator = new NabTAE(detector, scoreTarget)

  override def getTargetAlgorithmEvaluator(options: AbstractOptions): TargetAlgorithmEvaluator = new NabTAE(detector, scoreTarget)

  override def getTargetAlgorithmEvaluator(optionsMap: util.Map[String, AbstractOptions]): TargetAlgorithmEvaluator = new NabTAE(detector, scoreTarget)

  override def getOptionObject: AbstractOptions = new AbstractOptions {}
}
