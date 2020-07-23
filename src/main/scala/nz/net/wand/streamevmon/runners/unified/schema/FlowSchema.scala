package nz.net.wand.streamevmon.runners.unified.schema

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
case class FlowSchema(
  sources: Map[String, SourceInstance],
  detectors: Map[String, DetectorSchema],
  sinks    : Map[String, SinkInstance]
)
