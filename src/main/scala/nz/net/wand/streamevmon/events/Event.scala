package nz.net.wand.streamevmon.events

import java.time.Instant

import com.github.fsanaulla.chronicler.core.alias.ErrorOr
import com.github.fsanaulla.chronicler.core.model.InfluxWriter

/** Parent type for all anomalous events. These describe anomalies detected in
  * network measurements which may indicate that something is wrong with the
  * network.
  */
abstract class Event {

  /** Events should declare at least one tag that distinguishes them from other
    * events of the same type.
    *
    * If two events have the same type and tags, then one is liable to be lost.
    * No guarantees are provided of which is retained.
    */
  val tags: Map[String, String]

  /** Represents how likely an event is to represent a real issue. */
  val severity: Int

  /** The time that this event is considered to have started. */
  val eventTime: Instant

  /** The latency between this event starting and being detected, in milliseconds.
    * The detection time is considered to be the time of the measurement that
    * triggered the event to be emitted.
    */
  val detectionLatency: Int

  /** The name of the measurement, to be put into InfluxDB. Must be less than 64KB. */
  val measurementName: String

  /** The description of this instance of an event, to be put into InfluxDB. */
  val description: String

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
  def toLineProtocol: String
}

/** Conversion util for Chronicler database writes.
  */
object Event {
  implicit val writer: InfluxWriter[Event] = EventWriter()

  private case class EventWriter() extends InfluxWriter[Event] {
    override def write(obj: Event): ErrorOr[String] = Right(obj.toLineProtocol)
  }
}
