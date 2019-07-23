package nz.net.wand.amp.analyser.measurements

abstract class RichMeasurement extends Measurement {
  override def enrich(): Option[RichMeasurement] = Some(this)
}
