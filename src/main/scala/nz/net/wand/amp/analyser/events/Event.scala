package nz.net.wand.amp.analyser.events

import java.time.Instant

import org.apache.flink.streaming.connectors.influxdb.InfluxDBPoint

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

  /** Converts this object into a form that can be stored in InfluxDB.
    *
    * Used by [[nz.net.wand.amp.analyser.flink.InfluxSinkFunction InfluxSinkFunction]].
    */
  def asInfluxPoint: InfluxDBPoint
}
