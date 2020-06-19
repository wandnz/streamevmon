package nz.net.wand.streamevmon.connectors.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.{EventType, Summary}

/** A generic interface for discovering streams supported by an Esmond API host.
  */
abstract class AbstractEsmondStreamDiscovery {
  def discoverStreams(): Iterable[Either[EventType, Summary]]
}
