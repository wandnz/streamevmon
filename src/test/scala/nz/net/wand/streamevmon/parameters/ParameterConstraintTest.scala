package nz.net.wand.streamevmon.parameters

import nz.net.wand.streamevmon.TestBase
import nz.net.wand.streamevmon.parameters.constraints.{ParameterConstraint, ParameterSpecModifier}
import nz.net.wand.streamevmon.parameters.constraints.ParameterSpecModifier.ModifiedSpec

class ParameterConstraintTest extends TestBase {
  val zeroSpec = ParameterSpec("zero", 0, Some(-1), Some(1))
  val oneHundredSpec = ParameterSpec("one.hundred", 100, Some(50), Some(150))

  val zeroInstance = ParameterInstance(zeroSpec, 0)
  val oneHundredInstance = ParameterInstance(oneHundredSpec, 100)

  val zeroConst = new ParameterInstance.Constant(0)
  val oneHundredConst = new ParameterInstance.Constant(100)

  val oneHundredOver2Minus1 = new ModifiedSpec(
    ParameterSpec(
      "one.hundred.over.2.minus.1",
      100,
      Some(50),
      Some(150)
    ),
    ParameterSpecModifier.IntegralDivision(2),
    ParameterSpecModifier.Addition(-1)
  )
  val fortyNineInstance = ParameterInstance(oneHundredOver2Minus1, 49)

  "ParameterConstraint" should {
    "function with various types" in {
      val intSpecs = (
        ParameterSpec("i1", 0, Some(-1), Some(1)),
        ParameterSpec("i2", 100, Some(50), Some(150))
      )
      val longSpecs = (
        ParameterSpec("l1", 0L, Some(-1L), Some(1L)),
        ParameterSpec("l2", 100L, Some(50L), Some(150L))
      )
      val floatSpecs = (
        ParameterSpec("f1", 0.0f, Some(-1.0f), Some(1.0f)),
        ParameterSpec("f2", 100.0f, Some(50.0f), Some(150.0f))
      )
      val doubleSpecs = (
        ParameterSpec("d1", 0.0, Some(-1.0), Some(1.0)),
        ParameterSpec("d2", 100.0, Some(50.0), Some(150.0))
      )

      ParameterConstraint
        .LessThan(intSpecs._1, intSpecs._2)
        .apply(
          ParameterInstance(intSpecs._1, intSpecs._1.default),
          ParameterInstance(intSpecs._2, intSpecs._2.default)
        ) shouldBe true

      ParameterConstraint
        .LessThan(longSpecs._1, longSpecs._2)
        .apply(
          ParameterInstance(longSpecs._1, longSpecs._1.default),
          ParameterInstance(longSpecs._2, longSpecs._2.default)
        ) shouldBe true

      ParameterConstraint
        .LessThan(floatSpecs._1, floatSpecs._2)
        .apply(
          ParameterInstance(floatSpecs._1, floatSpecs._1.default),
          ParameterInstance(floatSpecs._2, floatSpecs._2.default)
        ) shouldBe true

      ParameterConstraint
        .LessThan(doubleSpecs._1, doubleSpecs._2)
        .apply(
          ParameterInstance(doubleSpecs._1, doubleSpecs._1.default),
          ParameterInstance(doubleSpecs._2, doubleSpecs._2.default)
        ) shouldBe true
    }

    "verify correctly" when {
      "regular instances supplied" in {
        ParameterConstraint.LessThan(
          zeroSpec,
          oneHundredSpec
        ).apply(zeroInstance, oneHundredInstance) shouldBe true

        ParameterConstraint.LessThan(
          oneHundredSpec,
          zeroSpec
        ).apply(oneHundredInstance, zeroInstance) shouldBe false

        ParameterConstraint.GreaterThan(
          zeroSpec,
          oneHundredSpec
        ).apply(zeroInstance, oneHundredInstance) shouldBe false

        ParameterConstraint.GreaterThan(
          oneHundredSpec,
          zeroSpec
        ).apply(oneHundredInstance, zeroInstance) shouldBe true

        ParameterConstraint.EqualTo(
          zeroSpec,
          zeroSpec
        ).apply(zeroInstance, zeroInstance) shouldBe true

        ParameterConstraint.EqualTo(
          zeroSpec,
          oneHundredSpec
        ).apply(zeroInstance, oneHundredInstance) shouldBe false
      }

      "constants supplied" in {
        ParameterConstraint.LessThan(
          zeroSpec,
          oneHundredConst.spec
        ).apply(zeroInstance, oneHundredConst) shouldBe true

        ParameterConstraint.LessThan(
          oneHundredConst.spec,
          zeroSpec
        ).apply(oneHundredConst, zeroInstance) shouldBe false

        ParameterConstraint.LessThan(
          oneHundredConst.spec,
          oneHundredConst.spec
        ).apply(oneHundredConst, oneHundredConst) shouldBe false

        ParameterConstraint.GreaterThan(
          zeroSpec,
          oneHundredConst.spec
        ).apply(zeroInstance, oneHundredConst) shouldBe false

        ParameterConstraint.GreaterThan(
          oneHundredConst.spec,
          zeroSpec
        ).apply(oneHundredConst, zeroInstance) shouldBe true

        ParameterConstraint.GreaterThan(
          oneHundredConst.spec,
          oneHundredConst.spec
        ).apply(oneHundredConst, oneHundredConst) shouldBe false

        ParameterConstraint.EqualTo(
          zeroSpec,
          zeroConst.spec
        ).apply(zeroInstance, zeroConst) shouldBe true

        ParameterConstraint.EqualTo(
          oneHundredConst.spec,
          zeroSpec
        ).apply(oneHundredConst, zeroInstance) shouldBe false

        ParameterConstraint.EqualTo(
          oneHundredConst.spec,
          oneHundredConst.spec
        ).apply(oneHundredConst, oneHundredConst) shouldBe true
      }

      "modified instances supplied" in {
        ParameterConstraint.LessThan(
          zeroSpec,
          oneHundredOver2Minus1
        ).apply(zeroInstance, fortyNineInstance) shouldBe true

        ParameterConstraint.LessThan(
          oneHundredOver2Minus1,
          zeroSpec
        ).apply(fortyNineInstance, zeroInstance) shouldBe false

        ParameterConstraint.GreaterThan(
          zeroSpec,
          oneHundredOver2Minus1
        ).apply(zeroInstance, fortyNineInstance) shouldBe false

        ParameterConstraint.GreaterThan(
          oneHundredOver2Minus1,
          zeroSpec
        ).apply(fortyNineInstance, zeroInstance) shouldBe true

        ParameterConstraint.EqualTo(
          zeroSpec,
          oneHundredOver2Minus1
        ).apply(zeroInstance, fortyNineInstance) shouldBe false

        ParameterConstraint.EqualTo(
          oneHundredOver2Minus1,
          oneHundredOver2Minus1
        ).apply(fortyNineInstance, fortyNineInstance) shouldBe true
      }
    }

    "recover and verify" when {
      "modified spec required but matching unmodified spec supplied" in {
        ParameterConstraint.LessThan(
          zeroSpec,
          oneHundredOver2Minus1
        ).apply(
          zeroInstance,
          ParameterInstance(ParameterSpec(oneHundredOver2Minus1.name, 100, Some(50), Some(150)), 100)
        ) shouldBe true

        ParameterConstraint.LessThan(
          oneHundredOver2Minus1,
          zeroSpec,
        ).apply(
          ParameterInstance(ParameterSpec(oneHundredOver2Minus1.name, 100, Some(50), Some(150)), 100),
          zeroInstance,
        ) shouldBe false
      }

      "matching modified specs supplied but not required" in {
        ParameterConstraint.LessThan(
          zeroSpec,
          oneHundredSpec
        ).apply(
          zeroInstance,
          ParameterInstance(
            new ModifiedSpec(
              oneHundredSpec,
              ParameterSpecModifier.IntegralDivision(2)
            ),
            100
          )
        ) shouldBe true

        ParameterConstraint.LessThan(
          oneHundredSpec,
          zeroSpec
        ).apply(
          ParameterInstance(
            new ModifiedSpec(
              oneHundredSpec,
              ParameterSpecModifier.IntegralDivision(2)
            ),
            100
          ),
          zeroInstance
        ) shouldBe false
      }

      "constants supplied but not required" in {
        ParameterConstraint.LessThan(
          zeroSpec,
          oneHundredSpec
        ).apply(zeroInstance, new ParameterInstance.Constant(100)) shouldBe true

        ParameterConstraint.LessThan(
          zeroSpec,
          oneHundredSpec
        ).apply(new ParameterInstance.Constant(0), oneHundredInstance) shouldBe true
      }
    }

    "fail to verify" when {
      "modified spec required but non-matching unmodified spec supplied" in {
        an[IllegalArgumentException] shouldBe thrownBy(
          ParameterConstraint.LessThan(
            zeroSpec,
            oneHundredOver2Minus1
          ).apply(zeroInstance, oneHundredInstance)
        )

        an[IllegalArgumentException] shouldBe thrownBy(
          ParameterConstraint.LessThan(
            oneHundredOver2Minus1,
            zeroSpec
          ).apply(oneHundredInstance, zeroInstance)
        )
      }

      "non-matching modified specs supplied but not required" in {
        an[IllegalArgumentException] shouldBe thrownBy(
          ParameterConstraint.LessThan(
            zeroSpec,
            oneHundredSpec
          ).apply(zeroInstance, fortyNineInstance)
        )

        an[IllegalArgumentException] shouldBe thrownBy(
          ParameterConstraint.LessThan(
            oneHundredSpec,
            zeroSpec
          ).apply(fortyNineInstance, zeroInstance)
        )
      }
    }
  }
}
