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

package nz.net.wand.streamevmon.tuner.nab.smac

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration
import ca.ubc.cs.beta.aeatk.algorithmrunresult.{AbstractAlgorithmRunResult, RunStatus}

/** NAB takes a lot of data for a run result. We'll provide as much as possible.
  */
case class NabAlgorithmRunResult(
  runConfiguration: AlgorithmRunConfiguration,
  acResult: RunStatus,
  runtime: Double,
  quality: Double,
  additionalRunData: String,
  wallClockTime    : Double
) extends AbstractAlgorithmRunResult(
  runConfiguration,
  acResult,
  runtime,
  0, // we don't care about this field
  quality,
  runConfiguration.getProblemInstanceSeedPair.getSeed, // ignored field
  "", // ignored
  false, // ignored
  additionalRunData,
  wallClockTime
) {
  override def kill(): Unit = {
    // We don't bother to implement anything for this - it's best effort, and
    // can only be called in very specific contexts.
  }
}
