package nz.net.wand.streamevmon.measurements

import java.time.Instant

/** Represents a single measurement at a point in time.
  */
abstract class Measurement extends Serializable {

  /** AMP measurements are tagged with a stream ID which corresponds to a
    * particular unique scheduled test.
    */
  val stream: Int

  /** The time at which the measurement occurred.
    */
  val time: Instant
}
