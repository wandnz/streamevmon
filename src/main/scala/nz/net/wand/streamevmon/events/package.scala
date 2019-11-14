package nz.net.wand.streamevmon

/** Contains classes representing detected anomalous events.
  * Any new events should inherit from [[nz.net.wand.streamevmon.events.Event Event]].
  * Any new algorithms will likely want to produce a custom Event type to ensure
  * uniqueness.
  */
package object events {}
