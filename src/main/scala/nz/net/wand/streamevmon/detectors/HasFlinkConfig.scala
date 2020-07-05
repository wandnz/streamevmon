package nz.net.wand.streamevmon.detectors

/** Inherited by detectors which want to set their own name and UID in a Flink pipeline.
  *
  * Required for use in the [[nz.net.wand.streamevmon.runners.UnifiedRunnerExtensions UnifiedRunner]].
  */
trait HasFlinkConfig {
  val detectorName: String
  val detectorUid: String
  val configKeyGroup: String
}
