package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.Measurement

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import org.apache.flink.api.common.io.{FileInputFormat, FilePathFilter}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.functions.source.SourceFunction

/** Represents a source. This will get built if it's required by any detectors.
  *
  * @param sourceType    The type of source, such as Influx or Esmond.
  * @param sourceSubtype The subtype of source where applicable, such as Amp or Bigdata.
  * @param config        Any configuration overrides that should be passed to the source.
  */
case class SourceInstance(
  @JsonProperty("type")
  @JsonScalaEnumeration(classOf[SourceTypeReference])
  sourceType: SourceType.ValueBuilder,
  @JsonProperty("subtype")
  @JsonScalaEnumeration(classOf[SourceSubtypeReference])
  sourceSubtype: Option[SourceSubtype.ValueBuilder],
  config: Map[String, String] = Map()
) {
  /** Builds the appropriate source with overridden config. */
  def buildSourceFunction: SourceFunction[Measurement] with HasFlinkConfig = {
    val configPrefixNoSubtype = s"source.$sourceType"
    val configPrefix = sourceSubtype.map(s => s"$configPrefixNoSubtype.$s")
      .getOrElse(configPrefixNoSubtype)

    sourceType
      .buildSourceFunction(sourceSubtype)
      .overrideConfig(config, configPrefix)
  }

  def buildFileInputFormat: (FileInputFormat[Measurement] with HasFlinkConfig, ParameterTool => FilePathFilter) = {
    val configPrefixNoSubtype = s"source.$sourceType"
    val configPrefix = sourceSubtype.map(s => s"$configPrefixNoSubtype.$s")
      .getOrElse(configPrefixNoSubtype)

    val (format, filter) = sourceType.buildFileInputFormat(sourceSubtype)

    format.setNestedFileEnumeration(true)

    (
      format.overrideConfig(config, configPrefix),
      filter
    )
  }
}
