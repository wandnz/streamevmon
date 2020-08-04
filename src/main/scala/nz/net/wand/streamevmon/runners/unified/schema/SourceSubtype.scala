package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.flink.sources._
import nz.net.wand.streamevmon.measurements.Measurement

import org.apache.flink.api.common.io.FileInputFormat
import org.apache.flink.streaming.api.functions.source.SourceFunction

/** This enum includes logic to build subtypes of sources. We must have subtypes
  * for all types within this enum, since we can't make an inheritance tree.
  */
object SourceSubtype extends Enumeration {

  class ValueBuilder(name: String) extends Val(name) {
    def buildSourceFunction(): SourceFunction[Measurement] with HasFlinkConfig = {
      val built = this match {
        case Amp => new AmpMeasurementSourceFunction()
        case Bigdata => new BigDataSourceFunction()
        case _ => throw new UnsupportedOperationException(s"Source subtype $this is not a SourceFunction")
      }
      built.asInstanceOf[SourceFunction[Measurement] with HasFlinkConfig]
    }

    def buildFileInputFormat(): FileInputFormat[Measurement] with HasFlinkConfig = {
      val built = this match {
        case LatencyTSAmp => new LatencyTSAmpFileInputFormat()
        case LatencyTSSmokeping => new LatencyTSSmokepingFileInputFormat()
        case _ => throw new UnsupportedOperationException(s"Source subtype $this is not a FileInputFormat")
      }
      built.asInstanceOf[FileInputFormat[Measurement] with HasFlinkConfig]
    }
  }

  val Amp: ValueBuilder = new ValueBuilder("amp")
  val Bigdata: ValueBuilder = new ValueBuilder("bigdata")
  val LatencyTSAmp: ValueBuilder = new ValueBuilder("ampicmp")
  val LatencyTSSmokeping: ValueBuilder = new ValueBuilder("smokeping")
}
