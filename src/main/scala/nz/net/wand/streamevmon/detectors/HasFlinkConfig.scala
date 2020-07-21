package nz.net.wand.streamevmon.detectors

import nz.net.wand.streamevmon.runners.unified.UnifiedRunnerExtensions

import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.api.java.utils.ParameterTool

/** Inherited by detectors which want to set their own name and UID in a Flink pipeline.
  *
  * Required for use in the [[UnifiedRunnerExtensions UnifiedRunner]].
  */
trait HasFlinkConfig {
  val flinkName: String
  val flinkUid: String
  val configKeyGroup: String

  protected var overrideParams: Option[ParameterTool] = None

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
