package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.events.Event

import org.apache.flink.streaming.api.functions.sink.SinkFunction

import scala.collection.mutable

class MockSink extends SinkFunction[Event] {
  override def invoke(value: Event, context: SinkFunction.Context[_]): Unit = {
    MockSink.received.append(value)
  }
}

object MockSink {
  val received: mutable.Buffer[Event] = mutable.Buffer()
}
