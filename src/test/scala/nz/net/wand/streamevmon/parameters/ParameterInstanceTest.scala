package nz.net.wand.streamevmon.parameters

import nz.net.wand.streamevmon.TestBase

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
