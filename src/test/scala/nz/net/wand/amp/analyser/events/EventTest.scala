package nz.net.wand.amp.analyser.events

import nz.net.wand.amp.analyser.SeedData

import org.scalatest.WordSpec

class EventTest extends WordSpec {
  "Children of Event.asInfluxPoint should work" when {
    "type is ThresholdEvent" in {
      val e = SeedData.thresholdEvent.influxPoint
      val r = SeedData.thresholdEvent.event.asInfluxPoint

      assertResult(e.getMeasurement)(r.getMeasurement)
      assertResult(e.getTimestamp)(r.getTimestamp)
      assertResult(e.getTags)(r.getTags)
      assertResult(e.getFields)(r.getFields)
    }
  }
}
