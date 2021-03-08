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

import nz.net.wand.streamevmon.test.TestBase

class ParameterInstanceTest extends TestBase {
  "ParameterInstance" should {
    "check validity for ordered specs" in {
      val spec = ParameterSpec(
        "spec",
        0,
        Some(-1),
        Some(1)
      )

      ParameterInstance(spec, 0).isValid shouldBe true
      ParameterInstance(spec, -1).isValid shouldBe true
      ParameterInstance(spec, 1).isValid shouldBe true
      ParameterInstance(spec, -2).isValid shouldBe false
      ParameterInstance(spec, 2).isValid shouldBe false
    }

    "be valid for unordered specs" in {
      val spec = ParameterSpec(
        "spec",
        Seq("an-element"),
        None,
        None
      )

      ParameterInstance(spec, Seq("a-different-element")).isValid shouldBe true
      ParameterInstance(spec, Seq("an-element")).isValid shouldBe true
    }

    "be valid for ordered non-numeric specs without limits set" in {
      // We use magic strings here since ParameterSpec has a special
      // case for numeric types (Int, Long, Double, Float) that requires
      // instances to set min and max. Strings are ordered, but not numeric.

      val noneSet = ParameterSpec("none.set", "e", None, None)
      val minSet = ParameterSpec("min.set", "e", Some("b"), None)
      val maxSet = ParameterSpec("max.set", "e", None, Some("y"))

      ParameterInstance(noneSet, "e").isValid shouldBe true
      ParameterInstance(noneSet, "a").isValid shouldBe true
      ParameterInstance(noneSet, "z").isValid shouldBe true
      ParameterInstance(minSet, "e").isValid shouldBe true
      ParameterInstance(minSet, "a").isValid shouldBe false
      ParameterInstance(minSet, "z").isValid shouldBe true
      ParameterInstance(maxSet, "e").isValid shouldBe true
      ParameterInstance(maxSet, "z").isValid shouldBe false
      ParameterInstance(maxSet, "a").isValid shouldBe true
    }

    "fail to create ordered numeric specs without limits set" in {
      an[IllegalArgumentException] shouldBe thrownBy(ParameterSpec("none.set", 0, None, None))
      an[IllegalArgumentException] shouldBe thrownBy(ParameterSpec("none.set", 0, Some(-1), None))
      an[IllegalArgumentException] shouldBe thrownBy(ParameterSpec("none.set", 0, None, Some(1)))
    }
  }
}
