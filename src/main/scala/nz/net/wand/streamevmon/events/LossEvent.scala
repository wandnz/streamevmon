package nz.net.wand.streamevmon.events

import java.time.Instant

/** Event objects are going to be revamped soon, so the implementation is
  * going to be nice and thin :)
  */
case class LossEvent(
    tags: Map[String, String],
    stream: Int,
    severity: Int,
    eventTime: Instant,
    description: String,
    detectionLatency: Long
) extends Event {

  final override val measurementName: String = LossEvent.measurementName

  override def toLineProtocol: String = ???
}

object LossEvent {
  final val measurementName = "loss_events"
}
