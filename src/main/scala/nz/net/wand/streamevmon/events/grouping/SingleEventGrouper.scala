package nz.net.wand.streamevmon.events.grouping

import nz.net.wand.streamevmon.events.Event

import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector

/** Converts events into groups that only contain a single event each. */
class SingleEventGrouper
  extends ProcessFunction[Event, EventGroup] {
  override def processElement(
    value                          : Event,
    ctx                            : ProcessFunction[Event, EventGroup]#Context,
    out                            : Collector[EventGroup]
  ): Unit = {
    out.collect(
      EventGroup(
        value.time.minus(value.detectionLatency),
        Some(value.time.minus(value.detectionLatency)),
        Seq(value)
      )
    )
  }
}
