package nz.net.wand.streamevmon.detectors

import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.api.java.utils.ParameterTool

/** Inherited by detectors which want to set their own name and UID in a Flink pipeline.
  *
  * Required for use in the [[nz.net.wand.streamevmon.runners.UnifiedRunnerExtensions UnifiedRunner]].
  */
trait HasFlinkConfig {
  val flinkName: String
  val flinkUid: String
  val configKeyGroup: String

  protected var overrideParams: Option[ParameterTool] = None

  def overrideConfig(config: ParameterTool): Unit = {
    overrideParams = Some(config)
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
