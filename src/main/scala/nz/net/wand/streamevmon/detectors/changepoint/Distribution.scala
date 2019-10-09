package nz.net.wand.streamevmon.detectors.changepoint

/** A trait mixed into classes representing continuous probability distributions
  * that evolve as more data is provided to them.
  *
  * @tparam T The type of object that the implementing class can accept as a
  *           new point to add to the model.
  */
trait Distribution[T] {

  /** A friendly name for this distribution. */
  val distributionName: String

  /** The probability density function, returning the relative likelihood for a
    * continuous random variable to take the value x.
    *
    * [[https://en.wikipedia.org/wiki/Probability_density_function]]
    */
  def pdf(x: Double): Double
  def pdf(x: T): Double

  /** Returns a new Distribution after adjustment for the new point added to it.
    * Reflects normal_distribution.updateStatistics */
  def withPoint(p: T, fakeN: Int): Distribution[T]

  val mean: Double
  val variance: Double
  val n: Int
}

object Distribution {

  def apply[T](dist: Distribution[T]): Distribution[T] = {
    dist match {
      case d: NormalDistribution[T] => NormalDistribution(d)
    }
  }
}
