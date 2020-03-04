package nz.net.wand.streamevmon.measurements

import java.time.Instant

case class SimpleIterableMeasurement(
  stream: Int,
  time: Instant,
  data: Iterable[Double]
) extends Measurement {

  override def isLossy: Boolean = data.isEmpty

  override def toCsvFormat: Iterable[String] = data.map(_.toString)

  override var defaultValue: Option[Double] = data.headOption

  override def defaultValues: Option[Iterable[Double]] =
    if (!isLossy) {
      Some(data)
    }
    else {
      None
    }

  def isAbnormal: Boolean = data.last != 0.0
}
