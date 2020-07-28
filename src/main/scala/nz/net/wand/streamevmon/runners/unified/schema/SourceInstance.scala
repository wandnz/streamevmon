package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.detectors.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.Measurement

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import org.apache.flink.api.common.io.FileInputFormat
import org.apache.flink.streaming.api.functions.source.SourceFunction

/** From-yaml representation of a source to build.
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

  def buildFileInputFormat: FileInputFormat[Measurement] with HasFlinkConfig = {
    val configPrefixNoSubtype = s"source.$sourceType"
    val configPrefix = sourceSubtype.map(s => s"$configPrefixNoSubtype.$s")
      .getOrElse(configPrefixNoSubtype)

    sourceType
      .buildFileInputFormat(sourceSubtype)
      .overrideConfig(config, configPrefix)
  }
}
