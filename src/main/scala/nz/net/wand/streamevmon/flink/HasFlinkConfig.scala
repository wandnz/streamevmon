package nz.net.wand.streamevmon.flink

import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.api.java.utils.ParameterTool

import scala.collection.JavaConverters._

/** Inherited by items which are intended for use in a Flink pipeline.
  *
  * Allows the inheritor to set a custom Flink operator name and UID, as well
  * as providing support for overriding the global configuration.
  */
trait HasFlinkConfig {
  val flinkName: String
  val flinkUid: String
  val configKeyGroup: String

  protected var overrideParams: Option[ParameterTool] = None

  def getOverrideParams: Option[ParameterTool] = overrideParams

  def overrideConfig(
    config   : Map[String, String],
    addPrefix: String = ""
  ): this.type = {
    if (config.nonEmpty) {
      if (addPrefix == "") {
        overrideConfig(ParameterTool.fromMap(config.asJava))
      }
      else {
        overrideConfig(ParameterTool.fromMap(config.map {
          case (k, v) => (s"$addPrefix.$k", v)
        }.asJava))
      }
    }
    this
  }

  def overrideConfig(config: ParameterTool): this.type = {
    overrideParams = Some(config)
    this
  }

  def configWithOverride(config: ParameterTool): ParameterTool = {
    overrideParams.fold {
      config
    } {
      p => config.mergeWith(p)
    }
  }

  def configWithOverride(context: RuntimeContext): ParameterTool = {
    configWithOverride(context.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool])
  }
}
