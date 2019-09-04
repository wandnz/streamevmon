package nz.net.wand.streamevmon.events

import nz.net.wand.streamevmon.SeedData
import nz.net.wand.streamevmon.SeedData.thresholdEvent

import org.scalatest.WordSpec

class EventTest extends WordSpec {

  "Events should become strings appropriately" when {
    "type is ThresholdEvent" when {
      "some tags are present" in {
        assert(thresholdEvent.withTagsAsString === SeedData.thresholdEvent.withTags.toString)
        assert(
          SeedData.thresholdEvent.withTagsAsLineProtocol === SeedData.thresholdEvent.withTags.toLineProtocol)
      }

      "no tags are present" in {
        assert(
          SeedData.thresholdEvent.withoutTagsAsString === SeedData.thresholdEvent.withoutTags.toString)
        assert(
          SeedData.thresholdEvent.withoutTagsAsLineProtocol === SeedData.thresholdEvent.withoutTags.toLineProtocol)
      }
    }
  }
}
