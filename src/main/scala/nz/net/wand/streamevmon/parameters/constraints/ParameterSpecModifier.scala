package nz.net.wand.streamevmon.parameters.constraints

import nz.net.wand.streamevmon.parameters.{ParameterInstance, ParameterSpec}

/** Allows modification of ParameterSpecs for usage in a ParameterConstraint
  * declaration.
  *
  * If you want to specify that parameterA is less than `(parameterB / 2) - 1`,
  * declare a constraint as follows:
  *
  * ``
  * ParameterConstraint.LessThan(
  * parameterA,
  * new ModifiedSpec(
  * parameterB,
  * ParameterSpecModifier.IntegralDivision(2),
  * ParameterSpecModifier.Addition(-1)
  * )
  * )
  * ``
  *
  * Note that subtraction is simply addition of a negative number.
  * Multiplication is not currently supported, but can be implemented by division
  * by a number less than 1, or division on the other side of the equation. It
  * will be added in the future if needed.
  *
  * Also note that a ModifiedSpec declaration can take an arbitrary number of
  * SpecModifiers. These are applied to the ParameterSpec in the order they are
  * passed - there is no reordering to respect the usual order of operations!
  */
object ParameterSpecModifier {

  /** Division of Integral numbers. Note that this is distinct from fractional
    * division.
    */
  case class IntegralDivision[T: Integral](denominator: T) extends SpecModifier[T](s"/ $denominator") {

    import Integral.Implicits._

    override def applyToValue(value: T): T = value / denominator
  }

  case class Addition[T: Numeric](right: T) extends SpecModifier[T](s"+ $right") {

    import Numeric.Implicits._

    override def applyToValue(value: T): T = value + right
  }

  /** A ParameterSpec that has a number of modifiers applied to it. The original
    * spec and the modifiers can be retrieved separately from the spec after it
    * has been modified.
    */
  class ModifiedSpec[T](
    val spec     : ParameterSpec[T],
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

  /** A single modifying operation to be applied to a ParameterSpec.
    *
    * @param name This should represent your operation in mathematical terms.
    *             For example, `/ 2` is a good name, but not `divide value by two`.
    */
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
