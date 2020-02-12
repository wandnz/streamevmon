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

  /** @return The object represented as a Seq as it would be in a CSV - simple
    *         types only. Collections should be converted to quoted strings
    *         separated by semicolons. Empty Options should become an empty
    *         string. Items should be organised in the order that columnNames
    *         gives. There is no need to be able to construct a Measurement
    *         object from the result of this method.
    */
  def toCsvFormat: Seq[String]

  /** @return The object, simplified in the way that toCsvFormat desires. */
  protected def toCsvTupleEntry(e: Any): String =
    e match {
      case o: Option[Any] => o.map(toCsvTupleEntry).getOrElse("").toString
      case s: Seq[Any] => s.map(toCsvTupleEntry).mkString("\"", ";", "\"")
      case i: Instant => i.toEpochMilli.toString
      case _ => e.toString
    }

  /** @return The value that is most likely to be useful to detectors. For
    *         example, an ICMP measurement might return its latency. This allows
    *         us to easily get a useful value from a flow of several measurement
    *         types without needing to know what each item is.
    */
  var defaultValue: Option[Double]
}
