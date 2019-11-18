package nz.net.wand.streamevmon.events

import nz.net.wand.streamevmon.SeedData

import org.scalatest.WordSpec

class EventTest extends WordSpec {

  "Events should become strings appropriately" when {
    "some tags are present" in {
      assert(SeedData.event.withTags.toString === SeedData.event.withTagsAsString)
      assert(SeedData.event.withTags.toLineProtocol === SeedData.event.withTagsAsLineProtocol)
    }

    "no tags are present" in {
      assert(SeedData.event.withoutTags.toString === SeedData.event.withoutTagsAsString)
      assert(SeedData.event.withoutTags.toLineProtocol === SeedData.event.withoutTagsAsLineProtocol)
    }
  }
}
