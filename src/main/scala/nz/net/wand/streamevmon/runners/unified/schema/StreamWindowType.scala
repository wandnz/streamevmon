package nz.net.wand.streamevmon.runners.unified.schema

import org.apache.flink.api.java.utils.ParameterTool

object StreamWindowType extends Enumeration {
  val Time: TimeWithOverrides = TimeWithOverrides(None)
  val Count: CountWithOverrides = CountWithOverrides(None, None)

  case class TimeWithOverrides(
    size: Option[ParameterTool => Long]
  ) extends Val("time")

  case class CountWithOverrides(
    size: Option[ParameterTool => Long],
    slide: Option[ParameterTool => Long]
  ) extends Val("count")

}
