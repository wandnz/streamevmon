package nz.net.wand.streamevmon.detectors.changepoint

import nz.net.wand.streamevmon.Logging

import org.scalactic.{Equality, TolerantNumerics}

/** An implementation of a normal distribution whose parameters are based on
  * the points provided to it.
  *
  * @param mapFunction The function to apply to elements of type T to obtain the
  *                    relevant data for this distribution. In the case of an
  *                    ICMP measurement, the relevant data is most likely the
  *                    latency.
  *
  * @see [[https://en.wikipedia.org/wiki/Normal_distribution]]
  */
case class NormalDistribution[T](
    data: Seq[Double] = Seq(),
    mapFunction: T => Double
) extends Distribution[T] with Logging {

  @transient implicit private[this] val doubleEquality: Equality[Double] =
    TolerantNumerics.tolerantDoubleEquality(1E-15)

  override def toString: String = {
    s"${getClass.getSimpleName}(n=$n,mean=$mean,variance=$variance)"
  }

  override val distributionName: String = "Normal Distribution"

  override def pdf(y: Double): Double = {
    n match {
      case 0 => 0.0
      case 1 =>
        logger.info("PDF called for n=1!")
        if (y == mean) {
          1.0
        }
        else {
          0.0
        }
      case _ =>
        if (variance == 0) {
          logger.info("PDF called when variance is 0!")
          if (y == mean) {
            1.0
          }
          else {
            0.0
          }
        }
        else {
          import java.lang.Math._
          val a = 1.0 / (sqrt(2.0 * PI) * sqrt(variance))
          a * exp(-(((y - mean) * (y - mean)) / (2.0 * variance)))
        }
    }
  }

  override def pdf(x: T): Double = {
    pdf(mapFunction(x))
  }

  /** Reflects normal_distribution.updateStatistics */
  override def withPoint(newT: T): NormalDistribution[T] = {
    NormalDistribution(
      data :+ mapFunction(newT),
      mapFunction
    )
  }

  override val n: Int = data.length

  private val sumPoints = data.sum
  private val sumSquarePoints = data.fold(0.0)((a, b) => a + (b * b))

  override val mean: Double = data.sum / n

  override val variance: Double = n match {
    case 0 | 1 => 0.0
    case _ =>
      (1.0 / (n * (n - 1))) * ((n * sumSquarePoints) - (sumPoints * sumPoints))
  }
}

object NormalDistribution {

  def apply[T](dist: NormalDistribution[T]): NormalDistribution[T] = {
    NormalDistribution(dist.data, dist.mapFunction)
  }

  def apply[T](item: T, mapFunction: T => Double): NormalDistribution[T] = {
    NormalDistribution(Seq(mapFunction(item)), mapFunction)
  }
}
