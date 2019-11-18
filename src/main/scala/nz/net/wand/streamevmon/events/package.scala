package nz.net.wand.streamevmon

/** Contains classes representing detected anomalous events.
  * Currently, there is only a single Event class, but types of events are
  * distinguished by the eventType field. The stream and any tags also serve to
  * make each type of event unique within InfluxDB or another storage system.
  */
package object events {}
