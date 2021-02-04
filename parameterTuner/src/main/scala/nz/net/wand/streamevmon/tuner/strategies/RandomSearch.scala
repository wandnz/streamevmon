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

package nz.net.wand.streamevmon.tuner.strategies

import nz.net.wand.streamevmon.parameters.{ParameterInstance, Parameters, ParameterSpec}
import nz.net.wand.streamevmon.tuner.jobs.JobResult

/** Generates random values for all parameters, except those with values set in
  * `fixedParameterValues`.
  */
case class RandomSearch(
  parameters: Seq[ParameterSpec[Any]],
  fixedParameterValues: Map[String, Any] = Map()
) extends SearchStrategy {
  override def nextParameters(lastResults: JobResult*): Parameters = {
    val newParams: Seq[ParameterInstance[Any]] = parameters
      .map { p =>
        if (fixedParameterValues.contains(p.name)) {
          ParameterInstance(p, fixedParameterValues(p.name))
        }
        else {
          p.generateRandomInRange()
        }
      }
    new Parameters(newParams: _*)
  }
}
