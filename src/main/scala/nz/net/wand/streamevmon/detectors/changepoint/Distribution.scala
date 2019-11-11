package nz.net.wand.streamevmon.detectors.changepoint

import org.apache.flink.api.common.typeinfo.TypeInformation

/** Mixed into classes representing continuous probability distributions
  * that evolve as more data is provided to them.
  *
  * @tparam T The type of object that the implementing class can accept as a
  *           new point to add to the model. Should be the same as the MeasT of
  *           [[ChangepointDetector]].
  *
  * @see [[NormalDistribution]]
  * @see [[MapFunction]]
  */
trait Distribution[T] {

  /** A friendly name for this distribution. */
  val distributionName: String

  /** The function to apply to elements of type T to obtain the relevant data
    * for this distribution. For example, an ICMP measurement would most likely
    * have its latency value extracted.
    */
  val mapFunction: MapFunction[T]

  /** The probability density function, returning the relative likelihood for a
    * continuous random variable to take the value that arises after applying
    * mapFunction to x.
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

/** The function that a [[Distribution]] implementation should supply to an
  * object of type T to convert it to a Double. Please note that
  * implementations of this class '''must be standalone named classes''', not
  * anonymous classes or inner classes. If either of these are used, the object
  * will not serialise correctly and your Flink job will not be able to use
  * checkpoints and savepoints correctly.
  *
  * apply(t: T): Double should contain your map function, while apply() only
  * needs to return a new instance of your implementation.
  *
  * @tparam T The type of object that the implementation of this class converts
  *           to a Double. Should be the same as the T supplied to Distribution.
  *
  * @see [[Distribution]]
  */
abstract class MapFunction[T: TypeInformation] extends Serializable {
  def apply(t: T): Double
  def apply(): MapFunction[T]
}
