package nz.net.wand.streamevmon.measurements.haberman

import nz.net.wand.streamevmon.measurements.Measurement

import java.time.Instant

/** Represents an entry from Haberman's survival dataset.
  *
  * @see [[https://archive.ics.uci.edu/ml/datasets/Haberman%27s+Survival]]
  *
  * @param stream         These should all be the same value for a single copy of the
  *                       dataset.
  * @param age            Age of patient at time of operation
  * @param year           Patient's year of operation
  * @param positiveNodes  Number of positive axillary nodes detected
  * @param survivalStatus The patient either did or did not survive 5 years or
  *                       more after the operation.
  * @param time           A dummy value. Time does not matter for this dataset, so it can
  *                       be set to whatever you find most useful for the Flink pipeline.
  */
case class Haberman(
  stream        : Int,
  age           : Int,
  year          : Int,
  positiveNodes : Int,
  survivalStatus: SurvivalStatus.Value,
  time          : Instant
) extends Measurement {

  override def isLossy: Boolean = false

  override def toCsvFormat: Seq[String] = Seq(
    age.toString,
    year.toString,
    positiveNodes.toString,
    survivalStatus.toString
  )

  override def toString: String = s"Haberman(${toCsvFormat.mkString(",")})"

  override var defaultValue: Option[Double] = Some(positiveNodes)

  override val defaultValues: Option[Seq[Double]] = Some(Seq(age, year, positiveNodes))
}

object SurvivalStatus extends Enumeration {
  val LessThan5Years: Value = Value(1)
  val MoreThan5Years: Value = Value(2)
}
