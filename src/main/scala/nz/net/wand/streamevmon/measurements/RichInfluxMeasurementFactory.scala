package nz.net.wand.streamevmon.measurements

import nz.net.wand.streamevmon.measurements.amp.PostgresMeasurementMeta

/** Mixed into companion objects of concrete [[RichInfluxMeasurement]] classes.
  *
  * @see [[InfluxMeasurementFactory]]
  */
trait RichInfluxMeasurementFactory {

  /** Creates a RichMeasurement by mixing an InfluxMeasurement with its associated
    * metadata.
    *
    * @param base The measurement.
    * @param meta The metadata associated with the measurement.
    *
    * @return The result if successful, or None.
    */
  def create(
    base: InfluxMeasurement,
    meta: PostgresMeasurementMeta
  ): Option[RichInfluxMeasurement]
}
