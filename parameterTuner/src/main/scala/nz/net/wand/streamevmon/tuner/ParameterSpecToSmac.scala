package nz.net.wand.streamevmon.tuner

import nz.net.wand.streamevmon.parameters.{DetectorParameterSpecs, ParameterSpec}
import nz.net.wand.streamevmon.parameters.constraints.ParameterConstraint
import nz.net.wand.streamevmon.runners.unified.schema.DetectorType

import java.io.{BufferedWriter, File, FileWriter}

import org.apache.commons.io.FileUtils

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
          s"${constraint.leftItem.toMathString} " +
          s"${getSmacOperator(constraint.operatorName)} " +
          s"${constraint.rightItem.toMathString} " +
          s"}"
        ).replace(".", "_")
    }
  }

  /** Creates a SMAC PCS file that specifies the bounds and restrictions of
    * available parameters.
    */
  def populateSmacParameterSpec(
    parameterSpecFile: String,
    detectors        : DetectorType.ValueBuilder*
  ): Unit = {
    // We only write the parameters for detectors we'll be using
    val allParameterSpecs = detectors.flatMap(DetectorParameterSpecs.parametersFromDetectorType)
    // We handle fixed parameters as single-field categorical variables to
    // ensure they don't have other values generated.
    val fixedParameters = DetectorParameterSpecs.fixedParameters

    FileUtils.forceMkdir(new File(parameterSpecFile).getParentFile)
    val writer = new BufferedWriter(new FileWriter(parameterSpecFile))

    // First we write down the simple specifications of parameter bounds.
    // toSmacString() takes an optional fixed parameter specification.
    allParameterSpecs.foreach { spec =>
      writer.write(spec.toSmacString(fixedParameters.get(spec.name)))
      writer.newLine()
    }

    // Throw a newline in so it's a little more readable.
    writer.newLine()

    // Let's now get the parameter restrictions for the detectors we're using.
    // Not all detectors even have restrictions, so this could well end out
    // being an empty list.
    // toSmacString() handles all the heavy lifting.
    val restrictions = detectors.flatMap(DetectorParameterSpecs.parameterRestrictionsFromDetectorType)
    restrictions.foreach { rest =>
      implicit val ev: Ordering[Any] = rest.ev
      writer.write(rest.toSmacString)
      writer.newLine()
    }

    // Flush it to make sure it's all written properly.
    writer.flush()
    writer.close()
  }
}
