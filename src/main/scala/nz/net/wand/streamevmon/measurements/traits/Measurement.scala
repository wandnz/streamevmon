package nz.net.wand.streamevmon.measurements.traits

import java.time.Instant

/** Represents a single measurement at a point in time.
  */
trait Measurement extends Serializable {

  /** Measurements have a stream ID which is used to distinguish entries from
    * separate time series. For example, amp measurements have a stream ID which
    * corresponds to a particular test schedule.
    */
  val stream: String

  /** The time at which the measurement occurred. */
  val time: Instant

  /** @return True if the object represents a measurement that encountered loss.
    *         If the type of measurement cannot record loss, this function will
    *         return false.
    */
  def isLossy: Boolean
}
