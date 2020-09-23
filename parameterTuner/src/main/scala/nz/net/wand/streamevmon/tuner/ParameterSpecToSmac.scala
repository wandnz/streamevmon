package nz.net.wand.streamevmon.tuner

import nz.net.wand.streamevmon.parameters.constraints.{ParameterConstraint, ParameterSpecModifier}
import nz.net.wand.streamevmon.parameters.ParameterSpec

/** This object includes some implicit methods to convert ParameterSpecs and
  * ParameterConstraints into strings compatible with SMAC's PCS file format.
  */
object ParameterSpecToSmac {

  implicit class SpecToSmac[T](spec: ParameterSpec[T]) {
    /** Converts this spec into the format expected by SMAC's PCS file. If the
      * fixedValue parameter is not None, the output is a categorical variable
      * with a single class of that value. Otherwise, a real or integer variable
      * is the output, which allows for a range of values.
      *
      * Note that the spec must be of type Int, Long, Float, or Double, and that
      * min and max must not be None.
      */
    def toSmacString(fixedValue: Option[T]): String = {
      // SMAC can't handle .s in parameter names if forbidden parameter syntax
      // is being used, so we'll replace them with something it can understand.
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

    /** This method can convert a constraint specification with several stacked
      * modifiers into a string that can be understood by SMAC with the correct
      * order of operations. For example, a constraint that first divides its
      * left element by two, then subtracts one should be converted to
      * `(x / 2) - 1`.
      */
    private def termToString(term: ParameterSpec[T]): String = {
      term match {
        // A constant numeric term just needs to be written down.
        case constant: ParameterSpec.Constant[_] => constant.default.toString
        case modified: ParameterSpecModifier.ModifiedSpec[_] =>
          // We iteratively stack modifiers here. Order of operations is
          // enforced by a liberal usage of brackets.
          val nameWithModifiers = modified.modifiers.foldLeft(modified.name) { (name, mod) =>
            s"($name ${mod.name})"
          }
          // Once we've applied all our modifiers, we can remove the last set
          // of brackets so it reads a little nicer. For example, `(x-1)` turns
          // into `x-1`.
          if (nameWithModifiers.startsWith("(")) {
            nameWithModifiers.drop(1).dropRight(1)
          }
          else {
            // This isn't needed if there are no brackets.
            nameWithModifiers
          }
        // If it's not a modified spec, just write down its name.
        case spec => spec.name
      }
    }

    /** SMAC inverts truth values for forbidden parameters. We represent
      * parameter restrictions like `x < 3` such that x must be less than 3 for
      * a parameter to be valid. SMAC's PCS file interprets that as "If x is
      * less than 3, the state is invalid". Hence, we reverse the operators.
      */
    private def getSmacOperator(operator: String): String = {
      operator match {
        case ">" => "<"
        case "<" => ">"
        case "==" => "!="
        case "!=" => "=="
      }
    }

    /** Converts a ParameterConstraint to an entry in a SMAC PCS file that
      * specifies a forbidden parameter.
      */
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
