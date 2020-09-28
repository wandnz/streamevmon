package nz.net.wand.streamevmon.parameters

import nz.net.wand.streamevmon.parameters.constraints.ParameterConstraint.ComparableConstraint

/** Allows a configurable class to expose its parameters in the form of a list
  * of [[ParameterSpec ParameterSpecs]], as well as a list of associated
  * [[nz.net.wand.streamevmon.parameters.constraints.ParameterConstraint constraints]].
  *
  * The `parametersAreValid` function can check if all the constraints of an
  * implementing class are satisfied by a given set of parameters.
  */
trait HasParameterSpecs {
  val parameterSpecs: Seq[ParameterSpec[Any]]

  val parameterRestrictions: Seq[ComparableConstraint[Any]]

  def parametersAreValid(params: Parameters): Boolean = parametersAreValid(params.elems)

  /** Checks that all of our restrictions are satisfied by the given set of
    * parameters. If a parameter that is used in a constraint is not passed,
    * the default is taken. This means that if no applicable values are passed,
    * all constraints will be tested against the default values specified by
    * this class - this should return true!
    */
  def parametersAreValid(params: Seq[ParameterInstance[Any]]): Boolean = {
    // For a set of parameters to be valid, none of our restrictions must be
    // violated.
    // If a parameter is not provided, we take the default.

    parameterRestrictions.map { restriction =>
      val lhs: ParameterInstance[Any] = params.find(_.name == restriction.leftItem.name) match {
        case Some(providedInstance) => providedInstance
        case None => parameterSpecs.find(_.name == restriction.leftItem.name) match {
          case Some(ourSpec) => ourSpec.getDefault
          case None => throw new IllegalStateException(s"Parameter restriction $restriction for unknown spec")
        }
      }

      val rhs: ParameterInstance[Any] = params.find(_.name == restriction.rightItem.name) match {
        case Some(providedInstance) => providedInstance
        case None => parameterSpecs.find(_.name == restriction.rightItem.name) match {
          case Some(ourSpec) => ourSpec.getDefault
          case None => throw new IllegalStateException(s"Parameter restriction $restriction for unknown spec")
        }
      }

      restriction.apply(lhs, rhs)
    }
      .forall(_ == true)
  }
}
