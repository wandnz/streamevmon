package nz.net.wand.streamevmon.connectors.esmond

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.connectors.esmond.schema.{EventType, Summary}

import java.time.{Duration, Instant}

import org.apache.flink.api.java.utils.ParameterTool

import scala.util.{Failure, Success}

class EsmondStreamDiscovery[ConnT <: AbstractEsmondConnection](
  configPrefix: String = "esmond.dataSource",
  params      : ParameterTool,
  esmond      : ConnT
) extends AbstractEsmondStreamDiscovery with Logging {

  lazy protected val timeRange: Duration = Duration.ofSeconds(params.getInt(s"$configPrefix.discoverTimeRange"))
  lazy protected val timeOffset: Duration = Duration.ofSeconds(params.getInt(s"$configPrefix.timeOffset"))
  lazy protected val source: Option[String] = Option(params.get(s"$configPrefix.source"))
  lazy protected val destination: Option[String] = Option(params.get(s"$configPrefix.destination"))
  lazy protected val measurementAgent: Option[String] = Option(params.get(s"$configPrefix.measurementAgent"))
  lazy protected val toolName: Option[String] = Option(params.get(s"$configPrefix.toolName"))
  lazy protected val dnsMatchRule: Option[String] = Option(params.get(s"$configPrefix.dnsMatchRule"))
  lazy protected val eventType: Option[String] = Option(params.get(s"$configPrefix.eventType"))

  def discoverStreams(): Iterable[Either[EventType, Summary]] = {
    val now = Instant.now().minus(timeOffset)
    val fullArchiveResult = esmond.getArchiveList(
      timeStart = Some(now.getEpochSecond - timeRange.getSeconds),
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
