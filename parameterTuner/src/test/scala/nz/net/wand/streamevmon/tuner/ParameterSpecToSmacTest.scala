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

package nz.net.wand.streamevmon.tuner

import nz.net.wand.streamevmon.parameters.ParameterSpec
import nz.net.wand.streamevmon.parameters.constraints.{ParameterConstraint, ParameterSpecModifier}
import nz.net.wand.streamevmon.parameters.constraints.ParameterSpecModifier.ModifiedSpec
import nz.net.wand.streamevmon.test.TestBase

import java.io.StringReader

import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace

class ParameterSpecToSmacTest extends TestBase {

  import nz.net.wand.streamevmon.tuner.ParameterSpecToSmac._

  val dummyName = getClass.getCanonicalName
  val smacName = dummyName.replace(".", "_")

  def testPcsParsesAndContainsNames(pcsFileText: String, names: Iterable[String] = Seq(smacName)): Unit = {
    try {
      val pcs = new ParameterConfigurationSpace(new StringReader(pcsFileText))
      names.foreach(name => pcs.getParameterNames should contain(name))
    }
    catch {
      case e: IllegalArgumentException => fail(s"PCS parsing failed! ${System.lineSeparator} $pcsFileText ${System.lineSeparator} $e")
      case e: Throwable => fail(s"Unknown exception during PCS parsing! $e")
    }
  }

  "ParameterSpecToSmac" should {
    "convert ParameterSpecs" in {
      Seq(
        (ParameterSpec(dummyName, 0, Some(-1), Some(1)), s"$smacName integer [-1,1] [0]"),
        (ParameterSpec(dummyName, 50L, Some(20L), Some(80L)), s"$smacName integer [20,80] [50]"),
        (ParameterSpec(dummyName, 42.24, Some(0.0), Some(100.0)), s"$smacName real [0.0,100.0] [42.24]"),
        (ParameterSpec(dummyName, 24.42f, Some(1.1f), Some(99.9f)), s"$smacName real [1.1,99.9] [24.42]"),
      )
        .foreach { case (spec, expected) =>
          spec.toSmacString(None) shouldBe expected
          testPcsParsesAndContainsNames(spec.toSmacString(None))
        }
    }

    "convert fixed ParameterSpecs" in {
      val spec = ParameterSpec(dummyName, 0, Some(-1), Some(1))
      spec.toSmacString(Some(1)) shouldBe s"$smacName categorical {1} [1]"
      testPcsParsesAndContainsNames(spec.toSmacString(Some(1)))
    }

    "convert ParameterConstraints" in {
      val dummyName2 = dummyName + ".secondName"
      val smacName2 = dummyName2.replace(".", "_")
      val lhs = ParameterSpec(dummyName, 100, Some(0), Some(200))
      val rhs = ParameterSpec(dummyName2, 0, Some(-1), Some(1))

      Seq(
        (ParameterConstraint.GreaterThan(lhs, rhs), s"{ $smacName < $smacName2 }"),
        (ParameterConstraint.LessThan(rhs, lhs), s"{ $smacName2 > $smacName }"),
        (ParameterConstraint.EqualTo(lhs, lhs), s"{ $smacName != $smacName }"),
        (ParameterConstraint.GreaterThan(
          new ModifiedSpec(
            lhs,
            ParameterSpecModifier.IntegralDivision(2),
            ParameterSpecModifier.Addition(-1)
          ),
          new ModifiedSpec(
            rhs,
            ParameterSpecModifier.Addition(1)
          )
        ), s"{ ($smacName / 2) + -1 < $smacName2 + 1 }"),
        (ParameterConstraint.EqualTo(new ParameterSpec.Constant(1), new ParameterSpec.Constant(1)), "{ 1 != 1 }")
      )
        .foreach { case (spec, expected) =>
          spec.toSmacString shouldBe expected
          val pcsFile = lhs.toSmacString(None) +
            System.lineSeparator() +
            rhs.toSmacString(None) +
            System.lineSeparator() +
            spec.toSmacString

          testPcsParsesAndContainsNames(pcsFile, Seq(smacName, smacName2))
        }
    }
  }
}
