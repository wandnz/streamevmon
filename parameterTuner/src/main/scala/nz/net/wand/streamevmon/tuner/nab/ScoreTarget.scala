package nz.net.wand.streamevmon.tuner.nab

object ScoreTarget extends Enumeration {
  val Standard: Value = Value("standard")
  val RewardLowFN: Value = Value("reward_low_FN_rate")
  val RewardLowFP: Value = Value("reward_low_FP_rate")
}
