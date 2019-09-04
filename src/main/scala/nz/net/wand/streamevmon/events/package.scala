package nz.net.wand.streamevmon

import nz.net.wand.streamevmon.events.Event

/** Contains classes representing detected anomalous events.
  * Any new events should inherit from [[Event Event]].
  * Any new algorithms will likely want to produce a custom Event type to ensure
  * uniqueness.
  */
package object events {}
