package nz.net.wand.streamevmon.parameters.constraints

import nz.net.wand.streamevmon.parameters.{ParameterInstance, ParameterSpec}
import nz.net.wand.streamevmon.Logging

import scala.Ordering.Implicits._

/** Specifies a constraint on a pair of ParameterSpecs.
  *
  * If you want to specify that parameterA is less than parameterB, declare a
  * constraint as follows:
  *
  * `ParameterConstraint.LessThan(parameterA, parameterB)`
  *
  * Note that parameterA and parameterB must have the same type T, and that T
  * must be an ordered type, such as Int.
  */
object ParameterConstraint {

  case class LessThan[T: Ordering](a: ParameterSpec[T], b: ParameterSpec[T]) extends ComparableConstraint[T]("<", a, b) {
    override def apply(a: T, b: T): Boolean = a < b
  }

  case class GreaterThan[T: Ordering](a: ParameterSpec[T], b: ParameterSpec[T]) extends ComparableConstraint[T](">", a, b) {
    override def apply(a: T, b: T): Boolean = a > b
  }

  case class EqualTo[T: Ordering](a: ParameterSpec[T], b: ParameterSpec[T]) extends ComparableConstraint[T]("==", a, b) {
    override def apply(a: T, b: T): Boolean = a == b
  }

  /** We only support constraints based on ordered comparisons at this point. */
  abstract class ComparableConstraint[T](
    val operatorName: String,
    val leftItem    : ParameterSpec[T],
    val rightItem   : ParameterSpec[T]
  )(implicit val ev: Ordering[T]) extends Logging {
    leftItem match {
      case _: ParameterSpec.Constant[_] => logger.error("Constraint verification with constant values are untested!")
      case _ =>
    }
    rightItem match {
      case _: ParameterSpec.Constant[_] => logger.error("Constraint verification with constant values are untested!")
      case _ =>
    }

    def apply(a: T, b: T): Boolean

    def apply(a: ParameterInstance[T], b: ParameterInstance[T]): Boolean = {
      val newA = a.spec match {
        case spec if spec.name == leftItem.name => a.value
        case _: ParameterSpec.Constant[_] => a.value
        case _ => throw new IllegalArgumentException(s"Instance was passed with a mismatched spec! Expected: $leftItem Got: ${a.spec}")
      }

      val newB = b.spec match {
        case spec if spec.name == rightItem.name => b.value
        case _: ParameterSpec.Constant[_] => b.value
        case _ => throw new IllegalArgumentException(s"Instance was passed with a mismatched spec! Expected: $rightItem Got: ${b.spec}")
      }

      this (newA, newB)
    }

    override def toString: String = s"${leftItem.toMathString} $operatorName ${rightItem.toMathString}"
  }
}
