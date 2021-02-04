/* This file is part of streamevmon.
 *
 * Copyright (C) 2021  The University of Waikato, Hamilton, New Zealand
 *
 * Author: Daniel Oosterwijk
 *
 * All rights reserved.
 *
 * This code has been developed by the University of Waikato WAND
 * research group. For further information please see https://wand.nz,
 * or our Github organisation at https://github.com/wanduow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.net.wand.streamevmon.parameters

import nz.net.wand.streamevmon.detectors.baseline.BaselineDetector
import nz.net.wand.streamevmon.detectors.changepoint.ChangepointDetector
import nz.net.wand.streamevmon.detectors.distdiff.DistDiffDetector
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.detectors.spike.SpikeDetector
import nz.net.wand.streamevmon.parameters.constraints.ParameterConstraint.ComparableConstraint
import nz.net.wand.streamevmon.runners.unified.schema.DetectorType

import org.apache.flink.api.java.utils.ParameterTool

import scala.collection.JavaConverters._

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

  /** Checks that all of our restrictions are satisfied by the given set of
    * parameters. If a parameter that is used in a constraint is not passed,
    * the default is taken. This means that if no applicable values are passed,
    * all constraints will be tested against the default values specified by
    * this class - this should return true!
    */
  def parameterInstancesAreValid(params: Seq[ParameterInstance[Any]], throwException: Boolean = false): Boolean = {
    val invalidParameters = params.filterNot(_.isValid)
    if (invalidParameters.nonEmpty) {
      throw new IllegalArgumentException(s"Invalid parameters! " +
        s"${invalidParameters.map(p => s"$p (min ${p.spec.min.getOrElse("none")}, max ${p.spec.max.getOrElse("none")})").mkString(", ")}")
    }

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

      val result = restriction.apply(lhs, rhs)
      if (!result && throwException) {
        throw new IllegalArgumentException(
          s"Parameter constraint was violated! " +
            s"{ $restriction } (Got values " +
            s"${lhs.name} = ${lhs.value}, " +
            s"${rhs.name} = ${rhs.value})"
        )
      }
      else {
        result
      }
    }
      .forall(_ == true)
  }

  def parametersAreValid(params: Parameters, throwException: Boolean = false): Boolean =
    parameterInstancesAreValid(params.elems, throwException)

  def parameterToolIsValid(params: ParameterTool, throwException: Boolean = false): Boolean =
    parameterInstancesAreValid(HasParameterSpecs.parameterInstancesFromParameterTool(params), throwException)
}

object HasParameterSpecs {
  /** Maps DetectorType ValueBuilders onto the appropriate companion object that
    * implements HasParameterSpecs.
    */
  val supportedTypesMap: Map[DetectorType.ValueBuilder, HasParameterSpecs] = Map(
    DetectorType.Baseline -> BaselineDetector,
    DetectorType.Changepoint -> ChangepointDetector,
    DetectorType.DistDiff -> DistDiffDetector,
    DetectorType.Mode -> ModeDetector,
    DetectorType.Spike -> SpikeDetector,
  )

  /** A list of supported companion objects that specify ParameterSpecs.
    */
  val supportedTypes: Seq[HasParameterSpecs] = supportedTypesMap.values.toSeq

  /** Gets all the ParameterSpecs for a particular DetectorType. */
  def parametersFromDetectorType(t: DetectorType.ValueBuilder): Iterable[ParameterSpec[Any]] =
    supportedTypesMap(t).parameterSpecs

  /** Gets all the parameter constraints for a particular DetectorType. */
  def parameterRestrictionsFromDetectorType(t: DetectorType.ValueBuilder): Iterable[ComparableConstraint[Any]] =
    supportedTypesMap(t).parameterRestrictions

  /** Gets all the parameters for all supported detectors. */
  val getAllDetectorParameters: Seq[ParameterSpec[Any]] =
    supportedTypes.flatMap(_.parameterSpecs)

  /** Checks whether the given list of parameters is fully valid. If any
    * detector finds a violation of one of its constraints, this function will
    * return false.
    */
  def parameterInstancesAreValid(params: Seq[ParameterInstance[Any]], throwException: Boolean = false): Boolean =
    supportedTypes.forall(_.parameterInstancesAreValid(params, throwException))

  def parametersAreValid(params: Parameters, throwException: Boolean = false): Boolean =
    parameterInstancesAreValid(params.elems, throwException)

  def parameterToolIsValid(params: ParameterTool, throwException: Boolean = false): Boolean =
    parameterInstancesAreValid(parameterInstancesFromParameterTool(params), throwException)

  private def parameterInstancesFromParameterTool(params: ParameterTool): Seq[ParameterInstance[Any]] = {
    params.toMap.asScala.flatMap { case (k, v) =>
      getAllDetectorParameters
        .find(_.name == k)
        .map { spec =>
          spec.default match {
            case _: Int => ParameterInstance(spec.asInstanceOf[ParameterSpec[Int]], v.toInt)
            case _: Long => ParameterInstance(spec.asInstanceOf[ParameterSpec[Long]], v.toLong)
            case _: Float => ParameterInstance(spec.asInstanceOf[ParameterSpec[Float]], v.toFloat)
            case _: Double => ParameterInstance(spec.asInstanceOf[ParameterSpec[Double]], v.toDouble)
          }
        }
    }.toSeq.asInstanceOf[Seq[ParameterInstance[Any]]]
  }

  /** Certain parameters aren't useful to anyone but the end user. We provide
    * default values for a number of them, for uses like parameter tuning.
    */
  val fixedParameters: Map[String, Any] = Map(
    "detector.baseline.inactivityPurgeTime" -> Int.MaxValue,
    "detector.changepoint.inactivityPurgeTime" -> Int.MaxValue,
    "detector.distdiff.inactivityPurgeTime" -> Int.MaxValue,
    "detector.mode.inactivityPurgeTime" -> Int.MaxValue,
    "detector.spike.inactivityPurgeTime" -> Int.MaxValue,
  )
}
