package nz.net.wand.amp.analyser.measurements

import java.time.Instant

abstract class Measurement {
  def enrich(): Option[RichMeasurement]

  val stream: Int
  val time: Instant
}
