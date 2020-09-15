package nz.net.wand.streamevmon.parameters.constraints

import nz.net.wand.streamevmon.parameters.{ParameterInstance, ParameterSpec}

object ParameterSpecModifier {

  case class IntegralDivision[T: Integral](denominator: T) extends SpecModifier[T](s"divided_by_$denominator") {

    import Integral.Implicits._

    override def applyToValue(value: T): T = value / denominator
  }

  case class Addition[T: Numeric](right: T) extends SpecModifier[T](s"add_$right") {

    import Numeric.Implicits._

    override def applyToValue(value: T): T = value + right
  }

  abstract class SpecModifier[T](
    val name: String
  ) {
    def applyToValue(value: T): T

    def applyToSpec(spec: ParameterSpec[T]): ParameterSpec[T] = ParameterSpec(
      spec.name,
      applyToValue(spec.default),
      spec.min.map(applyToValue),
      spec.max.map(applyToValue)
    )

    def applyToInstance(instance: ParameterInstance[T]): ParameterInstance[T] = {
      ParameterInstance(
        applyToSpec(instance.spec),
        applyToValue(instance.value)
      )
    }
  }

}
