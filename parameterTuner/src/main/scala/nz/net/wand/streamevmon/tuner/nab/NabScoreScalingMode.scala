package nz.net.wand.streamevmon.tuner.nab

import com.beust.jcommander.IStringConverter

object NabScoreScalingMode extends Enumeration {
  val Binary: ScalingValue = new ScalingValue("binary") {
    override def scale(input: Int): Double = {
      if (input == 0) {
        0.0
      }
      else {
        1.0
      }
    }
  }

  val Continuous: ScalingValue = new ScalingValue("continuous") {
    override def scale(input: Int): Double = input.toDouble / 100.0
  }

  abstract class ScalingValue(name: String) extends Val(name) {
    def scale(input: Int): Double
  }

  class Converter extends IStringConverter[ScalingValue] {
    override def convert(s: String): ScalingValue = {
      NabScoreScalingMode.withName(s).asInstanceOf[ScalingValue]
    }
  }

}
