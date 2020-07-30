package nz.net.wand.streamevmon.runners.unified

import nz.net.wand.streamevmon.{Configuration, SeedData, TestBase}

class SchemaParseTest extends TestBase {
  "Yaml Dag Schema" should {
    "parse correctly" in {
      val conf = Configuration.getFlowsDag(Some(getClass.getClassLoader.getResourceAsStream("flows.yaml")))

      conf shouldBe SeedData.flowDag.expectedSchema
    }
  }
}
