package nz.net.wand.streamevmon.detectors

import org.scalactic.{Equality, TolerantNumerics}

// TODO: The SumSquarePoints algorithm might not work well - we should
// investigate the growth of the double to ensure it doesn't get too large.
/** An implementation of a normal distribution whose parameters are based on
  * the points provided to it.
  *
  * @param sumPoints The sum of all points that have been observed as part of
  *                  the distribution.
  * @param sumSquarePoints The sum of the squares of all points observed.
  * @param n The number of points observed.
  * @param mapFunction The function to apply to elements of type T to obtain the
  *                    relevant data for this distribution. In the case of an
  *                    ICMP measurement, the relevant data is most likely the
  *                    latency.
  *
  * @see [[https://en.wikipedia.org/wiki/Normal_distribution]]
  */
case class NormalDistribution[T](
    sumPoints: Double = 0.0,
    sumSquarePoints: Double = 0.0,
    n: Int = 0,
    mapFunction: T => Double
) extends Distribution[T] {

  @transient implicit private[this] val doubleEquality: Equality[Double] =
    TolerantNumerics.tolerantDoubleEquality(1E-15)

  override def toString: String = {
    s"${getClass.getSimpleName}(n=$n,mean=$mean,variance=$variance)"
  }

  override val distributionName: String = "Normal Distribution"

  override def pdf(x: T): Double = {
    val y = mapFunction(x)
    n match {
      case 0 => 0.0
      case 1 =>
        if (y == mean) {
          1.0
        }
        else {
          0.0
        }
      case _ =>
        import java.lang.Math._
        val a = 1.0 / (sqrt(2.0 * PI) * sqrt(variance))
        a * exp(-(((y - mean) * (y - mean)) / (2.0 * variance)))
    }
  }

  /** Reflects normal_distribution.updateStatistics */
  override def withPoint(newT: T): NormalDistribution[T] = {
    val p = mapFunction(newT)
    NormalDistribution(
      sumPoints = sumPoints + p,
      sumSquarePoints = sumSquarePoints + (p * p),
      n = n + 1,
      mapFunction
    )
  }

  override lazy val mean: Double = sumPoints / n

  override lazy val variance: Double = n match {
    case 0 | 1 => 0.0
    case _ =>
      (1.0 / (n * (n - 1))) * ((n * sumSquarePoints) - (sumPoints * sumPoints))
  }
}

object NormalDistribution {
  def apply[T](dist: NormalDistribution[T]): NormalDistribution[T] = {
    dist match {
      case d: NormalDistribution[T] =>
        NormalDistribution(d.sumPoints, d.sumSquarePoints, d.n, d.mapFunction)
    }
  }
}
