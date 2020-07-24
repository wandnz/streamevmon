package nz.net.wand.streamevmon.detectors

import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.api.java.utils.ParameterTool

import scala.collection.JavaConverters._

/** Inherited by detectors which want to set their own name and UID in a Flink pipeline.
  */
trait HasFlinkConfig {
  val flinkName: String
  val flinkUid: String
  val configKeyGroup: String

  protected var overrideParams: Option[ParameterTool] = None

  def overrideConfig(
    config: Map[String, String],
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

  protected def configWithOverride(config: ParameterTool): ParameterTool = {
    overrideParams.fold {
      config
    } {
      p => config.mergeWith(p)
    }
  }

  protected def configWithOverride(context: RuntimeContext): ParameterTool = {
    configWithOverride(context.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool])
  }
}
