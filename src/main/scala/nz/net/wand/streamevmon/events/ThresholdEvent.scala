package nz.net.wand.streamevmon.events

import java.time.Instant
import java.util.concurrent.TimeUnit

/** Represents a simple threshold anomaly, such as a ping test having a higher
  * latency than expected.
  */
case class ThresholdEvent(
  tags: Map[String, String] = Map(),
  severity: Int,
  eventTime: Instant,
  detectionLatency: Long,
  description: String
) extends Event {

  final override val measurementName: String = ThresholdEvent.measurementName

  override def toLineProtocol: String = {
    s"${getTagString(tags)}${
      if (tags.nonEmpty) {
        " "
      }
      else {
        ""
      }
    }" +
      s"""severity=${severity}i,detection_latency=${detectionLatency}i,description="$description" """ +
      TimeUnit.MILLISECONDS.toNanos(eventTime.toEpochMilli)
  }

  override def toString: String = {
    s"$measurementName" + {
      if (tags.nonEmpty) {
        ","
      }
      else {
        " "
      }
    } +
      toLineProtocol
  }
}

object ThresholdEvent {
  final val measurementName = "threshold_events"
}
