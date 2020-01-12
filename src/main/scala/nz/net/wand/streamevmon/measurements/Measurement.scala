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

  /** @return True if the object represents a measurement that encountered loss.
    *         If the type of measurement cannot record loss, this function will
    *         return false.
    */
  def isLossy: Boolean

  /** @return The value that is most likely to be useful to detectors. For
    *         example, an ICMP measurement might return its latency. This allows
    *         us to easily get a useful value from a flow of several measurement
    *         types without needing to know what each item is.
    */
  var defaultValue: Option[Double]
}
