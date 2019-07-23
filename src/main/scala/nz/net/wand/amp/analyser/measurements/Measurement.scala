package nz.net.wand.amp.analyser.measurements

abstract class Measurement {
  def enrich(): Option[RichMeasurement]
}
