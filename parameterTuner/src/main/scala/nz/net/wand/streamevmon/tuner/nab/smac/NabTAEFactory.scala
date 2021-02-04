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

import nz.net.wand.streamevmon.runners.unified.schema.DetectorType
import nz.net.wand.streamevmon.tuner.nab.ScoreTarget

import java.util

import ca.ubc.cs.beta.aeatk.options.AbstractOptions
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.{TargetAlgorithmEvaluator, TargetAlgorithmEvaluatorFactory}

/** Creates NabTAE instances, which are in charge of creating and evaluating
  * runs.
  */
class NabTAEFactory extends TargetAlgorithmEvaluatorFactory {

  private lazy val detectors = System
    .getProperty("nz.net.wand.streamevmon.tuner.detectors")
    .split(",")
    .map(DetectorType.withName)
    .map(_.asInstanceOf[DetectorType.ValueBuilder])

  private lazy val scoreTargets = System
    .getProperty("nz.net.wand.streamevmon.tuner.scoreTargets")
    .split(",")
    .map(ScoreTarget.withName)

  private lazy val baseOutputDir = System.getProperty("nz.net.wand.streamevmon.tuner.runOutputDir")

  override def getName: String = "NAB"

  override def getTargetAlgorithmEvaluator: TargetAlgorithmEvaluator = new NabTAE(detectors, scoreTargets, baseOutputDir)

  override def getTargetAlgorithmEvaluator(options: AbstractOptions): TargetAlgorithmEvaluator = new NabTAE(detectors, scoreTargets, baseOutputDir)

  override def getTargetAlgorithmEvaluator(optionsMap: util.Map[String, AbstractOptions]): TargetAlgorithmEvaluator = new NabTAE(detectors, scoreTargets, baseOutputDir)

  override def getOptionObject: AbstractOptions = new AbstractOptions {}
}
