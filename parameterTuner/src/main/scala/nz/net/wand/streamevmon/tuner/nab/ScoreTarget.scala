package nz.net.wand.streamevmon.tuner.nab

/** NAB provides three scoring methods for every detector, which reward various
  * elements of detection. FN = False Negative, FP = False Positive.
  */
object ScoreTarget extends Enumeration {
  val Standard: Value = Value("standard")
  val RewardLowFN: Value = Value("reward_low_FN_rate")
  val RewardLowFP: Value = Value("reward_low_FP_rate")
}
