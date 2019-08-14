package nz.net.wand.amp.analyser.measurements

/** Represents the metadata associated with a set of measurements.
  */
abstract class MeasurementMeta {

  /** The stream ID used to identify the scheduled test this metadata is for.
    */
  val stream: Int
}
