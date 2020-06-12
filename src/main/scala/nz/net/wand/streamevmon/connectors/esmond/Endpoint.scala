package nz.net.wand.streamevmon.connectors.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.{EventType, Summary}

import java.time.Instant

case class Endpoint(
  details: Either[EventType, Summary],
  lastMeasurementTime: Instant
)
