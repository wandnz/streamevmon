package nz.net.wand.streamevmon.flink

import java.lang.{Long => JLong}
import java.time.Instant

import org.apache.flink.streaming.api.functions.sink.SinkFunction.Context

class MockSinkContext[T](
  time: Instant
) extends Context[T] {
  override def currentProcessingTime(): Long = time.toEpochMilli

  override def currentWatermark(): Long = time.toEpochMilli

  override def timestamp(): JLong = time.toEpochMilli
}
