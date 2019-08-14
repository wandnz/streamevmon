package nz.net.wand.amp.analyser.measurements

/** Represents a particular measurement at a point in time, complete with its
  * associated [[MeasurementMeta metadata]].
  */
abstract class RichMeasurement extends Measurement {

  /** A rich object cannot be further enriched, so this inherited method does nothing.
    *
    * @return This object.
    */
  override def enrich(): Option[RichMeasurement] = Some(this)
}
