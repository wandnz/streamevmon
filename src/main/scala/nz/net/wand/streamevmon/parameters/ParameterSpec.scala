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

import nz.net.wand.streamevmon.parameters.constraints.ParameterSpecModifier
import nz.net.wand.streamevmon.Perhaps

import org.apache.commons.math3.random.RandomDataGenerator

/** Specifies the valid range of values for a parameter.
  *
  * @param name    The full name of the parameter. For example,
  *                `detector.changepoint.triggerCount`, not just `triggerCount`.
  * @param default The default value for the parameter.
  * @param min     The minimum value of the detector, or None for unordered types.
  * @param max     The maximum value of the detector, or None for unordered types.
  * @tparam T The type of the parameter's value. Note that many features, such
  *           as random generation of ParameterInstances, rely on T being an
  *           Int, Long, Double, or Float. Other types will need additional
  *           handling that is not yet implemented.
  */
case class ParameterSpec[T](
  name   : String,
  default: T,
  min    : Option[T],
  max    : Option[T]
)(
  implicit val ordering: Perhaps[Ordering[T]]
) extends Serializable {

  default match {
    case _: Int | _: Long | _: Double | _: Float =>
      if (min.isEmpty || max.isEmpty) {
        throw new IllegalArgumentException(s"Must specify min and max values for parameter of type ${default.getClass.getSimpleName}")
      }
    case _ =>
  }

  def getDefault: ParameterInstance[T] = ParameterInstance(this, default)

  /** Generates a new instance containing a value between min and max according
    * to the RandomDataGenerator provided. This allows random generation
    * according to non-uniform distributions.
    */
  def generateRandomInRange(rand: RandomDataGenerator = new RandomDataGenerator()): ParameterInstance[T] = {
    val result = default match {
      case _: Int => rand.nextInt(min.get.asInstanceOf[Int], max.get.asInstanceOf[Int])
      case _: Long => rand.nextLong(min.get.asInstanceOf[Long], max.get.asInstanceOf[Long])
      case _: Double => rand.nextUniform(min.get.asInstanceOf[Double], max.get.asInstanceOf[Double])
      case _ => throw new UnsupportedOperationException(s"Can't generate random parameter of type ${default.getClass.getSimpleName}")
    }
    ParameterInstance(this, result.asInstanceOf[T])
  }

  /** Creates a mathematical representation of the ParameterSpec. For Constants,
    * we just return the value. For regular ParameterSpecs, we return the name.
    *
    * For a ModifiedSpec, we add the names of the SpecModifiers in order,
    * surrounding each level of modification with brackets to preserve the
    * order the operations are applied in.
    *
    * For example, a ModifiedSpec that first divides its value by two, then
    * subtracts one should be represented as `(x / 2) - 1`.
    */
  def toMathString: String = {
    this match {
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
}

object ParameterSpec {

  /** A ParameterSpec with only one legal value. Can be used in
    * ParameterConstraints.
    */
  class Constant[T](value: T) extends ParameterSpec[T](
    value.toString,
    value,
    Some(value),
    Some(value)
  )
}
