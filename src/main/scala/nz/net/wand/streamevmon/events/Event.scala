package nz.net.wand.streamevmon.events

import java.time.Instant

import com.github.fsanaulla.chronicler.core.alias.ErrorOr
import com.github.fsanaulla.chronicler.core.model.InfluxWriter

/** Parent type for all anomalous events. These describe anomalies detected in
  * network measurements which may indicate that something is wrong with the
  * network.
  */
// TODO: Should this be called Anomaly instead?
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

  /** The time associated with this event. */
  // TODO: Should this specify event time or detection time? Should we include both?
  val time: Instant

  /** The name of the measurement, to be put into InfluxDB. */
  val measurementName: String

  /** Converts a Map of tags into the relevant portion of a Line Protocol Format
    * string.
    *
    * @param t The tags to parse.
    *
    * @see [[https://docs.influxdata.com/influxdb/v1.7/write_protocols/line_protocol_reference/]]
    */
  protected def getTagString(t: Map[String, String]): String =
    tags.map({ case (k, v) => s"$k=$v" }).mkString(",")

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
