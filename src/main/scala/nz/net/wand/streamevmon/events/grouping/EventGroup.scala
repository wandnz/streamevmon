package nz.net.wand.streamevmon.events.grouping

import nz.net.wand.streamevmon.events.Event

import java.time.Instant

/** A group of events. If `endTime` is none, then the event is still ongoing. */
case class EventGroup(
  startTime: Instant,
  endTime  : Option[Instant],
  events   : Iterable[Event]
)
