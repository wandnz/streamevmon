package nz.net.wand.amp.analyser.events

import nz.net.wand.amp.analyser.SeedData

import org.scalatest.WordSpec

class EventTest extends WordSpec {
  "Children of Event.asInfluxPoint should work" when {
    "type is ThresholdEvent" in {
      val e = SeedData.thresholdEvent.influxPoint
      val r = SeedData.thresholdEvent.event.asInfluxPoint

      assert(r.getMeasurement === e.getMeasurement)
      assert(r.getTimestamp === e.getTimestamp)
      assert(r.getTags === e.getTags)
      assert(r.getFields === e.getFields)
    }
  }
}
