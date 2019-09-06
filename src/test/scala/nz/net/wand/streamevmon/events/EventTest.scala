package nz.net.wand.streamevmon.events

import nz.net.wand.streamevmon.SeedData

import org.scalatest.WordSpec

class EventTest extends WordSpec {

  "Events should become strings appropriately" when {
    "type is ThresholdEvent" when {
      "some tags are present" in {
        assert(SeedData.thresholdEvent.withTags.toString === SeedData.thresholdEvent.withTagsAsString)
        assert(SeedData.thresholdEvent.withTags.toLineProtocol === SeedData.thresholdEvent.withTagsAsLineProtocol)
      }

      "no tags are present" in {
        assert(SeedData.thresholdEvent.withoutTags.toString === SeedData.thresholdEvent.withoutTagsAsString)
        assert(SeedData.thresholdEvent.withoutTags.toLineProtocol === SeedData.thresholdEvent.withoutTagsAsLineProtocol)
      }
    }

    "type is ChangepointEvent" when {
      "some tags are present" in {
        assert(SeedData.changepointEvent.withTags.toString === SeedData.changepointEvent.withTagsAsString)
        assert(SeedData.changepointEvent.withTags.toLineProtocol === SeedData.changepointEvent.withTagsAsLineProtocol)
      }

      "no tags are present" in {
        assert(SeedData.changepointEvent.withoutTags.toString === SeedData.changepointEvent.withoutTagsAsString)
        assert(SeedData.changepointEvent.withoutTags.toLineProtocol === SeedData.changepointEvent.withoutTagsAsLineProtocol)
      }
    }
  }
}
