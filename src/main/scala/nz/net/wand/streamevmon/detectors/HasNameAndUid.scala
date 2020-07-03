package nz.net.wand.streamevmon.detectors

/** Inherited by detectors which want to set their own name and UID in a Flink
  * pipeline. Mostly used by the [[nz.net.wand.streamevmon.runners.UnifiedRunnerExtensions UnifiedRunner]].
  */
trait HasNameAndUid {
  val detectorName: String
  val detectorUid: String
}
