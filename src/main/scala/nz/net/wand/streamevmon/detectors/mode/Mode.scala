package nz.net.wand.streamevmon.detectors.mode

private[mode] case class Mode(value: Int, count: Int)

private[mode] object Mode {
  def apply(input: (Int, Int)): Mode = Mode(input._1, input._2)
}
