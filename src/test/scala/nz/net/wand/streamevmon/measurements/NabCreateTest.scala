package nz.net.wand.streamevmon.measurements

import nz.net.wand.streamevmon.{SeedData, TestBase}
import nz.net.wand.streamevmon.measurements.nab.NabMeasurement

class NabCreateTest extends TestBase {
  "Nab measurements" should {
    "be created" in {
      NabMeasurement("", SeedData.nab.exampleLine) shouldBe SeedData.nab.expected
    }

    "turn back into strings" in {
      SeedData.nab.expected.toString shouldBe SeedData.nab.exampleLine
    }
  }
}
