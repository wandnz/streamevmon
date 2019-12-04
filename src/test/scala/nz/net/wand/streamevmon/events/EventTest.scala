package nz.net.wand.streamevmon.events

import nz.net.wand.streamevmon.{SeedData, TestBase}

class EventTest extends TestBase {

  "Events should become strings appropriately" when {
    "some tags are present" in {
      SeedData.event.withTags.toString shouldBe SeedData.event.withTagsAsString
      SeedData.event.withTags.toLineProtocol shouldBe SeedData.event.withTagsAsLineProtocol
    }

    "no tags are present" in {
      SeedData.event.withoutTags.toString shouldBe SeedData.event.withoutTagsAsString
      SeedData.event.withoutTags.toLineProtocol shouldBe SeedData.event.withoutTagsAsLineProtocol
    }
  }
}
