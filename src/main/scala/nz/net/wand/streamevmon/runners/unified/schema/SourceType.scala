package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.connectors.esmond.{EsmondConnectionForeground, EsmondStreamDiscovery}
import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.flink.sources.PollingEsmondSourceFunction
import nz.net.wand.streamevmon.measurements.Measurement

import org.apache.flink.api.common.io.FileInputFormat
import org.apache.flink.streaming.api.functions.source.SourceFunction

/** This enum includes logic to build sources. */
object SourceType extends Enumeration {

  val Influx: ValueBuilder = new ValueBuilder("influx")
  val Esmond: ValueBuilder = new ValueBuilder("esmond")
  val LatencyTS: ValueBuilder = new ValueBuilder("latencyts")

  class ValueBuilder(name: String) extends Val(name) {
    def buildSourceFunction(
      subtype: Option[SourceSubtype.ValueBuilder]
    ): SourceFunction[Measurement] with HasFlinkConfig = {
      val source = this match {
        case Influx => subtype match {
          case Some(value) => value match {
            // Since we can't do enum inheritance, we have to manually check
            // valid subtypes for each type. The subtypes know how to build
            // themselves, so we'll let them do it.
            case SourceSubtype.Amp | SourceSubtype.Bigdata =>
              value.buildSourceFunction()
            case _ => throw new IllegalArgumentException(s"Cannot build $this type source with ${subtype.getOrElse("no")} subtype!")
          }
          case None => throw new IllegalArgumentException(s"Cannot build $this type source with no subtype!")
        }
        case Esmond =>
          // Esmond doesn't have subtypes.
          new PollingEsmondSourceFunction[
            EsmondConnectionForeground,
            EsmondStreamDiscovery[EsmondConnectionForeground]
          ]()
        case _ => throw new UnsupportedOperationException(s"Source type $this is not a SourceFunction")
      }
      source.asInstanceOf[SourceFunction[Measurement] with HasFlinkConfig]
    }

    def buildFileInputFormat(
      subtype: Option[SourceSubtype.ValueBuilder],
    ): FileInputFormat[Measurement] with HasFlinkConfig = {
      val result = this match {
        case LatencyTS =>
          subtype match {
            case Some(value) => value match {
              case SourceSubtype.LatencyTSAmp | SourceSubtype.LatencyTSSmokeping =>
                value.buildFileInputFormat()
              case _ => throw new IllegalArgumentException(
                s"Cannot build $this type source with ${subtype.getOrElse("no")} subtype!")
            }
            case None => throw new IllegalArgumentException(
              s"Cannot build $this type source with ${subtype.getOrElse("no")} subtype!")
          }
        case _ => throw new UnsupportedOperationException(s"Source type $this is not a FileInputFormat")
      }
      result.asInstanceOf[FileInputFormat[Measurement] with HasFlinkConfig]
    }
  }
}
