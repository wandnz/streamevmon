package nz.net.wand.streamevmon.runners.unified.schema

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/** The top-level class for a yaml-configured flow specification.
  *
  * This has `@JsonIgnoreProperties(ignoreUnknown = true)` set, allowing users
  * to provide their own top-level keys that get ignored. This lets them do
  * things like defining anchors on their own.
  */
@JsonIgnoreProperties(ignoreUnknown = true)
case class FlowSchema(
  sources: Map[String, SourceInstance],
  detectors: Map[String, DetectorSchema],
  sinks    : Map[String, SinkInstance]
)
