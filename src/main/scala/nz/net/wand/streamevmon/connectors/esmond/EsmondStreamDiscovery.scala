package nz.net.wand.streamevmon.connectors.esmond

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.connectors.esmond.schema.{EventType, Summary}

import java.time.Duration

import scala.util.{Failure, Success}

// TODO: Improve config. Especially need to ensure it respects timeOffset and fetchHistory.

class EsmondStreamDiscovery(
  configPrefix    : String = "esmond.dataSource",
  timeRange       : Duration = Duration.ofDays(1),
  esmond          : EsmondConnectionForeground,
  source          : Option[String] = None,
  destination     : Option[String] = None,
  measurementAgent: Option[String] = None,
  toolName        : Option[String] = None,
  dnsMatchRule    : Option[String] = None,
  eventType       : Option[String] = None
) extends Logging {

  def discoverStreams(): Iterable[Either[EventType, Summary]] = {
    val fullArchiveResult = esmond.getArchiveList(
      timeRange = Some(timeRange.getSeconds),
      source = source,
      destination = destination,
      measurementAgent = measurementAgent,
      toolName = toolName,
      dnsMatchRule = dnsMatchRule,
      eventType = eventType,
    )

    val fullArchive = fullArchiveResult match {
      case Failure(exception) => throw exception
      case Success(value) => value.toList
    }

    val events = fullArchive.flatMap(_.eventTypes)
    val summaries = events.flatMap(_.summaries)

    val e = events.map(Left(_))
    val s = summaries.map(Right(_))
    e.union(s)
  }
}
