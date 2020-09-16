package nz.net.wand.streamevmon.parameters.constraints

import nz.net.wand.streamevmon.parameters.{ParameterInstance, ParameterSpec}

object ParameterSpecModifier {

  case class IntegralDivision[T: Integral](denominator: T) extends SpecModifier[T](s"/ $denominator") {

    import Integral.Implicits._

    override def applyToValue(value: T): T = value / denominator
  }

  case class Addition[T: Numeric](right: T) extends SpecModifier[T](s"+ $right") {

    import Numeric.Implicits._

    override def applyToValue(value: T): T = value + right
  }

  class ModifiedSpec[T](
    val spec: ParameterSpec[T],
    val modifiers: SpecModifier[T]*
  ) extends ParameterSpec[T](
    spec.name,
    modifiers.foldLeft(spec.default)((value, mod) => mod.applyToValue(value)),
    modifiers.foldLeft(spec.min)((value, mod) => value.map(mod.applyToValue)),
    modifiers.foldLeft(spec.max)((value, mod) => value.map(mod.applyToValue))
  ) {

    override def canEqual(other: Any): Boolean = other.isInstanceOf[ModifiedSpec[T]]

    override def equals(other: Any): Boolean = other match {
      case that: ModifiedSpec[T] =>
        (that canEqual this) &&
          spec == that.spec &&
          modifiers == that.modifiers
      case _ => false
    }

    override def hashCode(): Int = {
      val state = Seq(spec, modifiers)
      state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
    }

    override def toString = s"ModifiedSpec($spec, $modifiers)"
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
