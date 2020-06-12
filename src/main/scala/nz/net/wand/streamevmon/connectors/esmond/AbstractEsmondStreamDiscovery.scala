package nz.net.wand.streamevmon.connectors.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.{EventType, Summary}

abstract class AbstractEsmondStreamDiscovery {
  def discoverStreams(): Iterable[Either[EventType, Summary]]
}
