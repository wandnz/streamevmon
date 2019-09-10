package nz.net.wand.streamevmon.detectors

import nz.net.wand.streamevmon.measurements.Measurement

import scala.collection.mutable

class RunHolder[MeasT <: Measurement, DistT <: Distribution[MeasT]](
    initialDistribution: DistT
) {
  var runs: mutable.Seq[(DistT, Double)] = mutable.Seq[(DistT, Double)]()

  def length: Int = runs.length
  def isEmpty: Boolean = runs.isEmpty

  def add(item: MeasT): Unit = {
    runs = runs :+ Tuple2(initialDistribution.withPoint(item), 1.0).asInstanceOf[(DistT, Double)]
  }

  def purge(): Unit = runs = mutable.Seq[(DistT, Double)]()

  def normalise(): Unit = {
    val total = runs
      .map(_._2)
      .fold(0.0)((a, b) => a + b)

    // There's a special case here if the total is 0, since it means the PDF for
    // all our runs is 0. This'll happen if there's an outlier that doesn't fit
    // in any existing runs.

    runs.zipWithIndex
      .foreach { x =>
        runs.update(x._2, (x._1._1, x._1._2 / total))
      }
  }

  /** Reflects cpp::249-272
    *
    * @return The probability that there was a change point.
    */
  def applyGrowthProbabilities(item: MeasT, hazard: Double = 1.0 / 200.0): Double = {
    // A smaller hazard tends to be less sensitive (larger lambda)
    var weight = 0.0

    println("===== Before")
    runs.foreach(print)
    println()

    // Update the probabilities of the run such that
    // runs[n+1].prob = runs[n].pdf * runs[n].prob * (1 - hazard)
    runs = runs
      .map { x =>
        (x._1.withPoint(item), x._2 * x._1.pdf(item) * (1 - hazard))
      }
      .asInstanceOf[mutable.Seq[(DistT, Double)]]

    weight = runs.map(_._2).fold(0.0)((a, b) => a + b)

    runs :+ Tuple2(initialDistribution.withPoint(item), weight)

    println("===== After")
    runs.foreach(print)
    println()

    weight
  }

  /** @return The distribution with the highest probability.
    */
  def getMostProbable: DistT = runs.maxBy(_._2)._1
}
