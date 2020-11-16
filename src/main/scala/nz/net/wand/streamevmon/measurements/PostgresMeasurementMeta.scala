package nz.net.wand.streamevmon.measurements

/** Represents the metadata associated with a set of measurements.
  */
trait PostgresMeasurementMeta extends MeasurementMeta {

  /** The stream ID used to identify the scheduled test this metadata is for.
    */
  val stream: Int
}
