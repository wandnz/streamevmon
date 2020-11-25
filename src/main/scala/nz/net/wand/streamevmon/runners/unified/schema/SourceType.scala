package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.connectors.esmond.{EsmondConnectionForeground, EsmondStreamDiscovery}
import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.flink.sources.{NabFileInputFormat, PollingEsmondSourceFunction}
import nz.net.wand.streamevmon.measurements.traits.Measurement

import org.apache.flink.api.common.io.{FileInputFormat, FilePathFilter, GlobFilePathFilter}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.core.fs.Path
import org.apache.flink.streaming.api.functions.source.SourceFunction

import scala.collection.JavaConverters._

/** This enum includes logic to build sources. It's usually deferred to the
  * relevant [[SourceSubtype]].
  */
object SourceType extends Enumeration {

  val Influx: ValueBuilder = new ValueBuilder("influx")
  val Esmond: ValueBuilder = new ValueBuilder("esmond")
  val LatencyTS: ValueBuilder = new ValueBuilder("latencyts")
  val NAB: ValueBuilder = new ValueBuilder("nab")

  class ValueBuilder(name: String) extends Val(name) {

    def buildSourceFunction(
      subtype: Option[SourceSubtype.ValueBuilder]
    ): SourceFunction[Measurement] with HasFlinkConfig = {
      val source = this match {
        case Influx => subtype match {
          case Some(value) => value match {
            case SourceSubtype.Amp | SourceSubtype.Bigdata => value.buildSourceFunction()
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
    ): (FileInputFormat[Measurement] with HasFlinkConfig, ParameterTool => FilePathFilter) = {
      val result = this match {
        case LatencyTS =>
          subtype match {
            case Some(value) => value match {
              case SourceSubtype.LatencyTSAmp | SourceSubtype.LatencyTSSmokeping =>
                val format = value.buildFileInputFormat()
                (
                  format,
                  (config: ParameterTool) => new GlobFilePathFilter(
                    Seq(config.get(s"source.${format.configKeyGroup}.files"))
                      .map(f => s"**/$f.series")
                      .asJava,
                    Seq().asJava
                  )
                )
              case _ => throw new IllegalArgumentException(
                s"Cannot build $this type source with ${subtype.getOrElse("no")} subtype!")
            }
            case None => throw new IllegalArgumentException(
              s"Cannot build $this type source with ${subtype.getOrElse("no")} subtype!")
          }
        case NAB =>
          (
            new NabFileInputFormat,
            (_: ParameterTool) => new FilePathFilter {
              override def filterPath(filePath: Path): Boolean = filePath.getPath.endsWith(".md")
            }
          )
        case _ => throw new UnsupportedOperationException(s"Source type $this is not a FileInputFormat")
      }
      result.asInstanceOf[(FileInputFormat[Measurement] with HasFlinkConfig, ParameterTool => FilePathFilter)]
    }
  }
}
