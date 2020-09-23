package nz.net.wand.streamevmon.tuner.nab

import com.beust.jcommander.IStringConverter

/** Specifies scaling modes to use when converting Event severities into NAB
  * anomaly likelihoods. Includes an IStringConverter to let JCommander create
  * these from command-line parameters.
  */
object NabScoreScalingMode extends Enumeration {
  /** Marks any events as 1.0 likelihood, regardless of severity. */
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

  /** Maps the range 0-100 directly onto 0-1, including decimal values. */
  val Continuous: ScalingValue = new ScalingValue("continuous") {
    override def scale(input: Int): Double = input.toDouble / 100.0
  }

  /** Declares the scale function for enum values. */
  abstract class ScalingValue(name: String) extends Val(name) {
    def scale(input: Int): Double
  }

  /** Allows JCommander to parse command-line parameters as enum values. */
  class Converter extends IStringConverter[ScalingValue] {
    override def convert(s: String): ScalingValue = {
      NabScoreScalingMode.withName(s).asInstanceOf[ScalingValue]
    }
  }

}
