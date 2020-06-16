package nz.net.wand.streamevmon.measurements.amp

/** Represents the metadata associated with a set of measurements.
  */
abstract class MeasurementMeta {

  /** The stream ID used to identify the scheduled test this metadata is for.
    */
  val stream: Int
}
