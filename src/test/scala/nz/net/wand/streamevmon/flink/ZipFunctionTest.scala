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

package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.test.HarnessingTest

import org.apache.flink.streaming.api.scala.createTypeInformation

class ZipFunctionTest extends HarnessingTest {
  "ZipFunction" should {
    "send measurements appropriately" in {
      val func = new ZipFunction[Int, String]()
      val harness = newHarness(func)
      harness.open()

      Range(0, 100)
        .map(i => (i, i.toString))
        .foreach { case (int, string) =>
          harness.processElement1(int, int.toLong)
          harness.processElement2(string, int.toLong)
        }

      Range(100, 200)
        .foreach { i =>
          harness.processElement1(i, i.toLong)
        }
      Range(100, 300)
        .foreach { i =>
          harness.processElement2(i.toString, i.toLong)
        }
      Range(200, 300)
        .foreach { i =>
          harness.processElement1(i, i.toLong)
        }

      Range(0, 300)
        .foreach { i =>
          harness.extractOutputValues.get(i) shouldBe(i, i.toString)
        }
    }

    "recover from checkpoints" in {
      val func1 = new ZipFunction[Int, String]()
      val harness1 = newHarness(func1)
      harness1.open()

      Range(0, 100)
        .foreach { i =>
          harness1.processElement1(i, i.toLong)
        }

      val checkpoint1 = harness1.snapshot(1, 100)
      val func2 = new ZipFunction[Int, String]()
      val harness2 = newHarness(func2)
      harness2.initializeState(checkpoint1)
      harness2.open()

      Range(0, 100)
        .foreach { i =>
          harness2.processElement2(i.toString, i.toLong)
          harness2.extractOutputValues.get(i) shouldBe(i, i.toString)
        }
      Range(100, 200)
        .foreach { i =>
          harness2.processElement2(i.toString, i.toLong)
        }

      val checkpoint2 = harness2.snapshot(2, 200)
      val func3 = new ZipFunction[Int, String]()
      val harness3 = newHarness(func2)
      harness3.initializeState(checkpoint2)
      harness3.open()

      Range(100, 200)
        .foreach { i =>
          harness3.processElement1(i, i.toLong)
          harness3.extractOutputValues.get(i - 100) shouldBe(i, i.toString)
        }
    }
  }
}
