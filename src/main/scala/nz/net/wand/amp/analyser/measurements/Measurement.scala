package nz.net.wand.amp.analyser.measurements

import java.time.Instant

/** Represents a single measurement at a point in time.
  */
abstract class Measurement {

  /** Obtains the metadata which is relevant to this measurement.
    *
    * @return An object representing this measurement complete with the metadata.
    */
  def enrich(): Option[RichMeasurement]

  /** AMP measurements are tagged with a stream ID which corresponds to a
    * particular unique scheduled test.
    */
  val stream: Int

  /** The time at which the measurement occurred.
    */
  val time: Instant
}
