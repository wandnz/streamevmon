package nz.net.wand.streamevmon.detectors.changepoint

import nz.net.wand.streamevmon.measurements.Measurement

/** Mixed into classes representing continuous probability distributions
  * that evolve as more data is provided to them.
  *
  * @tparam T The type of object that the implementing class can accept as a
  *           new point to add to the model. Should be the same as the MeasT of
  *           [[ChangepointDetector]].
  *
  * @see [[NormalDistribution]]
  */
trait Distribution[T <: Measurement] {

  /** A friendly name for this distribution. */
  val distributionName: String

  /** The probability density function, returning the relative likelihood for a
    * continuous random variable to take the value of x.defaultValue.
    *
    * [[https://en.wikipedia.org/wiki/Probability_density_function]]
    */
  def pdf(x: T): Double

  /** Returns a new Distribution after adjustment for the new point added to it. */
  def withPoint(p: T, newN: Int): Distribution[T]

  val mean: Double
  val variance: Double
  val n: Int
}
