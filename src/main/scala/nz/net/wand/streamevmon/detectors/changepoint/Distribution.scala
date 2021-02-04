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

package nz.net.wand.streamevmon.detectors.changepoint

import nz.net.wand.streamevmon.measurements.traits.{HasDefault, Measurement}

/** Parent class for continuous probability distributions that evolve as more
  * data is provided to them.
  *
  * @tparam T The type of object that the implementing class can accept as a
  *           new point to add to the model. Should be the same as the MeasT of
  *           [[ChangepointDetector]].
  *
  * @see [[NormalDistribution]]
  */
trait Distribution[T <: Measurement with HasDefault] {

  /** A friendly name for this distribution. */
  val distributionName: String

  /** The probability density function, returning the relative likelihood for a
    * continuous random variable to take the value of x.defaultValue.
    *
    * [[https://en.wikipedia.org/wiki/Probability_density_function]]
    */
  def pdf(x: T): Double

  /** Returns a new Distribution after adjustment for the new point added to it. */
  def withPoint(p: T, newN: Int): Distribution[T]

  val mean: Double
  val variance: Double
  val n: Int
}
