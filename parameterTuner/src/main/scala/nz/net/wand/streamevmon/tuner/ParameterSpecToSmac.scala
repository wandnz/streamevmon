package nz.net.wand.streamevmon.tuner

import nz.net.wand.streamevmon.parameters.constraints.{ParameterConstraint, ParameterSpecModifier}
import nz.net.wand.streamevmon.parameters.ParameterSpec

object ParameterSpecToSmac {

  implicit class SpecToSmac[T](spec: ParameterSpec[T]) {
    /** Converts this spec into the format expected by SMAC's PCS file. If the
      * fixedValue parameter is not None, the output is a categorical variable
      * with a single class of that value. Otherwise, this spec is replicated,
      * allowing for a range of variable values.
      */
    def toSmacString(fixedValue: Option[T]): String = {
      val safeName = spec.name.replace(".", "_")
      fixedValue match {
        case Some(value) => s"$safeName categorical {$value} [$value]"
        case None =>
          val specType = spec.default match {
            case _: Int | _: Long => "integer"
            case _: Float | _: Double => "real"
            case _ => throw new UnsupportedOperationException(s"Can't create SMAC spec for spec with type ${spec.default.getClass.getCanonicalName}")
          }
          (spec.min, spec.max) match {
            case (Some(_), Some(_)) =>
            case _ => throw new UnsupportedOperationException(s"Must specify min and max for SMAC spec")
          }
          s"$safeName $specType [${spec.min.get},${spec.max.get}] [${spec.default}]"
      }
    }
  }

  implicit class RestrictionToSmac[T: Ordering](constraint : ParameterConstraint.ComparableConstraint[T]) {
    private def termToString(term: ParameterSpec[T]): String = {
      term match {
        case constant: ParameterSpec.Constant[_] => constant.default.toString
        case modified: ParameterSpecModifier.ModifiedSpec[_] =>
          val nameWithModifiers = modified.modifiers.foldLeft(modified.name) { (name, mod) =>
            s"($name ${mod.name})"
          }
          if (nameWithModifiers.startsWith("(")) {
            nameWithModifiers.drop(1).dropRight(1)
          }
          else {
            nameWithModifiers
          }
        case spec => spec.name
      }
    }

    private def getSmacOperator(operator: String): String = {
      operator match {
        case ">" => "<"
        case "<" => ">"
        case "==" => "!="
        case "!=" => "=="
      }
    }

    def toSmacString: String = {
      (
        s"{ " +
          s"${termToString(constraint.leftItem)} " +
          s"${getSmacOperator(constraint.operatorName)} " +
          s"${termToString(constraint.rightItem)} " +
          s"}"
        ).replace(".", "_")
    }
  }

}
