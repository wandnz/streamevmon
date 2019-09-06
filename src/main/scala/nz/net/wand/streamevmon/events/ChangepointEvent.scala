package nz.net.wand.streamevmon.events

import java.time.Instant
import java.util.concurrent.TimeUnit

case class ChangepointEvent(
  tags                 : Map[String, String] = Map(),
  stream               : Int,
  severity             : Int,
  eventTime            : Instant,
  detectionLatency: Int,
  description: String
) extends Event {
  final override val measurementName: String = ChangepointEvent.measurementName

  override def toLineProtocol: String = {
    s"${getTagString(tags + ("stream" -> stream.toString))} " +
      s"""severity=${severity}i,detection_latency=${detectionLatency}i,description="$description" """ +
      TimeUnit.MILLISECONDS.toNanos(eventTime.toEpochMilli)
  }

  override def toString: String = {
    s"$measurementName,$toLineProtocol"
  }
}

object ChangepointEvent {
  final val measurementName = "changepoint_events"
}
