package nz.net.wand.streamevmon.connectors.esmond

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.connectors.esmond.schema.{EventType, Summary}

import java.time.{Duration, Instant}

import org.apache.flink.api.java.utils.ParameterTool

import scala.util.{Failure, Success}

/** Discovers streams supported by an Esmond API host.
  *
  * == Configuration ==
  *
  * This class is configured by the `esmond.dataSource` config key group, which
  * is used to filter the results returned by the API. As such, many keys are
  * optional. Many of these keys have additional details in [[EsmondAPI.archiveList]].
  *
  * - `discoverTimeRange`: The length of time to discover streams from, in seconds.
  * Default 86400, or 1 day.
  *
  * - `timeOffset`: The number of seconds in the past to report results from.
  * For example, if set to 86400 (1 day), streams from between 2 days and 1 day
  * ago will be discovered.
  * Default 0.
  *
  * - `limit`: The number of archives to be returned. Each archive might contain
  * many streams.
  * Optional. The API default is 1000.
  *
  * - `offset`: Used in conjunction with `limit`, the API will skip this many
  * values before returning any.
  * Optional. The API default is 0.
  *
  * - `source` and `destination`: The hosts which took part in the test. Goes
  * through DNS resolution.
  * Optional.
  *
  * - `measurementAgent` and `toolName`: Details about the test procedure. Useful
  * for filtering for certain types of tests only.
  * Optional.
  *
  * - `eventType`, `sumaryType`, and `summaryWindow`: Details about the specific
  * tests desired.
  * Optional.
  *
  * - `dnsMatchRule`: How to resolve addresses provided to `source` and `destination`.
  * Optional.  `v4v6` is default, and uses both. `only-v4`, `only-v6`, `prefer-v4`, and `prefer-v6` are other options.
  *
  * @param configPrefix A custom config prefix to use. This might be useful for
  *                     discovering streams from multiple hosts.
  * @param params       The configuration to use. Generally obtained from the Flink
  *                     global configuration.
  * @param esmond       The Esmond connection to use.
  * @tparam ConnT The type of the Esmond connection to use. This can usually be
  *               obtained implicitly.
  */
class EsmondStreamDiscovery[ConnT <: AbstractEsmondConnection](
  configPrefix: String = "esmond.dataSource",
  params      : ParameterTool,
  esmond      : ConnT
) extends AbstractEsmondStreamDiscovery with Logging {
  lazy protected val timeRange: Duration = Duration.ofSeconds(params.getInt(s"$configPrefix.discoverTimeRange"))
  lazy protected val timeOffset: Duration = Duration.ofSeconds(params.getInt(s"$configPrefix.timeOffset"))
  lazy protected val limit: Option[Int] = Option(params.getInt(s"$configPrefix.limit"))
  lazy protected val offset: Option[Int] = Option(params.getInt(s"$configPrefix.offset"))
  lazy protected val source: Option[String] = Option(params.get(s"$configPrefix.source"))
  lazy protected val destination: Option[String] = Option(params.get(s"$configPrefix.destination"))
  lazy protected val measurementAgent: Option[String] = Option(params.get(s"$configPrefix.measurementAgent"))
  lazy protected val toolName: Option[String] = Option(params.get(s"$configPrefix.toolName"))
  lazy protected val dnsMatchRule: Option[String] = Option(params.get(s"$configPrefix.dnsMatchRule"))
  lazy protected val eventType: Option[String] = Option(params.get(s"$configPrefix.eventType"))
  lazy protected val summaryType: Option[String] = Option(params.get(s"$configPrefix.summaryType"))
  lazy protected val summaryWindow: Option[Long] = Option(params.getLong(s"$configPrefix.summaryWindow"))

  def discoverStreams(): Iterable[Either[EventType, Summary]] = {
    // TODO: Support querying for multiple event types, among other config fields.
    // This will probably need multiple API calls.
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
