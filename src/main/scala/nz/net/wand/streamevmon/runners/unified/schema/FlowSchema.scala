package nz.net.wand.streamevmon.runners.unified.schema

case class FlowSchema(
  sources: Map[String, SourceInstance],
  detectors: Map[String, DetectorSchema],
  sinks: Map[String, SinkInstance]
)
