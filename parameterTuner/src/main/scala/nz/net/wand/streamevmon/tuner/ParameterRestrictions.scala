package nz.net.wand.streamevmon.tuner

import nz.net.wand.streamevmon.detectors.changepoint.ChangepointDetector
import nz.net.wand.streamevmon.parameters.ParameterSpec
import nz.net.wand.streamevmon.parameters.constraints.{ParameterConstraint, ParameterSpecModifier}
import nz.net.wand.streamevmon.parameters.constraints.ParameterSpecModifier.ModifiedSpec

object ParameterRestrictions {
  def main(args: Array[String]): Unit = {

    import nz.net.wand.streamevmon.tuner.ParameterSpecToSmac._

    val maxHistory: ParameterSpec[Int] = ChangepointDetector.parameterSpecs.find(_.name == "detector.changepoint.maxHistory").get
    val triggerCount = ChangepointDetector.parameterSpecs.find(_.name == "detector.changepoint.triggerCount").get
    val aLessThanB = ParameterConstraint.LessThan(triggerCount, maxHistory)

    val aGreaterThan3 = ParameterConstraint.GreaterThan(maxHistory, new ParameterSpec.Constant[Int](3))

    val divideBy2 = ParameterSpecModifier.IntegralDivision(2)
    val subtract10 = ParameterSpecModifier.Addition(-10)

    val aOver2Minus10 = new ModifiedSpec(
      maxHistory,
      divideBy2,
      subtract10
    )

    val aOver2Minus1GreaterThan3 = ParameterConstraint.GreaterThan(
      aOver2Minus10,
      new ParameterSpec.Constant(3)
    )

    val s = aOver2Minus1GreaterThan3.toSmacString

    val breakpoint = 1
  }
}
