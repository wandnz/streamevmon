package nz.net.wand.streamevmon.events

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit

import com.github.fsanaulla.chronicler.core.alias.ErrorOr
import com.github.fsanaulla.chronicler.core.model.InfluxWriter

/** Generic type for all anomalous events. These describe anomalies detected in
  * network measurements which may indicate that something is wrong with the
  * network.
  *
  * @param eventType        A short descriptor of the type of event being represented.
  *                         If the event is placed into InfluxDB, this field becomes
  *                         the measurement (table) name. As such, it is usually
  *                         pluralised.
  * @param stream           A stream ID, which becomes another distinguishing tag from
  *                         other events of the same type. It is good practice to set
  *                         this field to the same as the stream ID of the stream generating
  *                         the event where possible.
  * @param severity         How severe the event is, usually from 0-100.
  * @param time             The time that the event was detected at.
  * @param detectionLatency The time between the event beginning and being
  *                         detected. This can be used as an approximation of
  *                         the time the event started, which is difficult to
  *                         determine with some algorithms.
  * @param description      A user-facing description of the event.
  * @param tags             Additional tags allow events with the same eventType to distinguish
  *                         themselves from one another, if there are several subtypes. If
  *                         two events have the same type, stream, tags, and time, one is
  *                         liable to be lost.
  */
case class Event(
  eventType: String,
  stream: Int,
  severity: Int,
  time: Instant,
  detectionLatency: Duration,
  description: String,
  tags: Map[String, String]
) extends Serializable {

  /** Converts a Map of tags into the relevant portion of a Line Protocol Format
    * string.
    *
    * @param t The tags to parse.
    *
    * @see [[https://docs.influxdata.com/influxdb/v1.7/write_protocols/line_protocol_reference/]]
    */
  protected def getTagString(t: Map[String, String]): String =
    t.map({ case (k, v) => s"$k=$v" }).mkString(",")

  /** Converts the Event into InfluxDB Line Protocol Format.
    *
    * @see [[https://docs.influxdata.com/influxdb/v1.7/write_protocols/line_protocol_reference/]]
    */
  def toLineProtocol: String = {
    s"${getTagString(tags + ("stream" -> stream.toString))} " +
      s"severity=${severity}i,detection_latency=${detectionLatency.toNanos}i," +
      s"""description="$description" """ +
      TimeUnit.MILLISECONDS.toNanos(time.toEpochMilli)
  }

  override def toString: String = {
    s"$eventType,$toLineProtocol"
  }
}

/** Conversion util for Chronicler database writes. */
object Event {
  def getWriter[T <: Event]: InfluxWriter[T] = EventWriter[T]()

  case class EventWriter[T <: Event]() extends InfluxWriter[T] {
    override def write(obj: T): ErrorOr[String] = Right(obj.toLineProtocol)
  }
}
