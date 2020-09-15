package nz.net.wand.streamevmon.parameters.constraints

import nz.net.wand.streamevmon.detectors.changepoint.ChangepointDetector
import nz.net.wand.streamevmon.parameters.ParameterSpec

class ParameterRestrictions {
  val maxHistory: ParameterSpec[Int] = ChangepointDetector.parameterSpecs.find(_.name == "detector.changepoint.maxHistory").get
  val triggerCount = ChangepointDetector.parameterSpecs.find(_.name == "detector.changepoint.triggerCount").get
  val aLessThanB = ParameterConstraint.LessThan(triggerCount, maxHistory)

  val aGreaterThan3 = ParameterConstraint.GreaterThan(maxHistory, new ParameterSpec.Constant[Int](3))

  val divideBy2 = ParameterSpecModifier.IntegralDivision(2)
  val subtract1 = ParameterSpecModifier.Addition(-1)

  val aOver2Minus1GreaterThan3 = ParameterConstraint.GreaterThan(
    subtract1.applyToSpec(
      divideBy2.applyToSpec(maxHistory)
    ),
    new ParameterSpec.Constant(3)
  )

  val breakpoint = 1
}
