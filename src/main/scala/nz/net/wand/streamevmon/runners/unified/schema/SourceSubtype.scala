package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.detectors.HasFlinkConfig
import nz.net.wand.streamevmon.flink.{AmpMeasurementSourceFunction, BigDataSourceFunction}
import nz.net.wand.streamevmon.measurements.Measurement

import org.apache.flink.streaming.api.functions.source.SourceFunction

/** This enum includes logic to build subtypes of sources. We must have subtypes
  * for all types within this enum, since we can't make an inheritance tree.
  */
object SourceSubtype extends Enumeration {

  class ValueBuilder(name: String) extends Val(name) {
    def build(): SourceFunction[Measurement] with HasFlinkConfig = {
      val built = this match {
        case Amp => new AmpMeasurementSourceFunction()
        case Bigdata => new BigDataSourceFunction()
      }
      built.asInstanceOf[SourceFunction[Measurement] with HasFlinkConfig]
    }
  }

  val Amp: ValueBuilder = new ValueBuilder("amp")
  val Bigdata: ValueBuilder = new ValueBuilder("bigdata")
}
