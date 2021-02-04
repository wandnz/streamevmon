/* This file is part of streamevmon.
 *
 * Copyright (C) 2020-2021  The University of Waikato, Hamilton, New Zealand
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
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.net.wand.streamevmon.tuner.nab

import com.beust.jcommander.IStringConverter

/** Specifies scaling modes to use when converting Event severities into NAB
  * anomaly likelihoods. Includes an IStringConverter to let JCommander create
  * these from command-line parameters.
  */
object NabScoreScalingMode extends Enumeration {
  /** Marks any events as 1.0 likelihood, regardless of severity. */
  val Binary: ScalingValue = new ScalingValue("binary") {
    override def scale(input: Int): Double = {
      if (input == 0) {
        0.0
      }
      else {
        1.0
      }
    }
  }

  /** Maps the range 0-100 directly onto 0-1, including decimal values. */
  val Continuous: ScalingValue = new ScalingValue("continuous") {
    override def scale(input: Int): Double = input.toDouble / 100.0
  }

  /** Declares the scale function for enum values. */
  abstract class ScalingValue(name: String) extends Val(name) {
    def scale(input: Int): Double
  }

  /** Allows JCommander to parse command-line parameters as enum values. */
  class Converter extends IStringConverter[ScalingValue] {
    override def convert(s: String): ScalingValue = {
      NabScoreScalingMode.withName(s).asInstanceOf[ScalingValue]
    }
  }

}
