package nz.net.wand.streamevmon.parameters.constraints

import nz.net.wand.streamevmon.parameters.{ParameterInstance, ParameterSpec}

import scala.Ordering.Implicits._

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

  abstract class ComparableConstraint[T: Ordering](
    val operatorName: String,
    val leftItem    : ParameterSpec[T],
    val rightItem   : ParameterSpec[T]
  ) {
    def apply(a: T, b: T): Boolean

    def apply(a: ParameterInstance[T], b: ParameterInstance[T]): Boolean = {
      if (a.spec != leftItem || b.spec != rightItem) {
        throw new IllegalArgumentException("Instances were passed with the wrong specs!")
      }

      this (a.value, b.value)
    }
  }

}
