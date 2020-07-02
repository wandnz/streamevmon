package nz.net.wand.streamevmon.connectors.esmond

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.connectors.esmond.schema.{EventType, Summary}

import java.time.{Duration, Instant}

import org.apache.flink.api.java.utils.ParameterTool

import scala.annotation.tailrec
import scala.collection.immutable.HashSet
import scala.util.{Failure, Success, Try}

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
  * - `eventType`, `summaryType`, and `summaryWindow`: Details about the specific
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
  lazy protected val limit: Option[Long] = Try(params.getLong(s"$configPrefix.limit")).toOption
  lazy protected val offset: Option[Long] = Try(params.getLong(s"$configPrefix.offset")).toOption
  lazy protected val source: Option[String] = Option(params.get(s"$configPrefix.source"))
  lazy protected val destination: Option[String] = Option(params.get(s"$configPrefix.destination"))
  lazy protected val measurementAgent: Option[String] = Option(params.get(s"$configPrefix.measurementAgent"))
  lazy protected val toolName: Option[String] = Option(params.get(s"$configPrefix.toolName"))
  lazy protected val dnsMatchRule: Option[String] = Option(params.get(s"$configPrefix.dnsMatchRule"))
  lazy protected val eventType: Option[String] = Option(params.get(s"$configPrefix.eventType"))
  lazy protected val summaryType: Option[String] = Option(params.get(s"$configPrefix.summaryType"))
  lazy protected val summaryWindow: Option[Long] = Try(params.getLong(s"$configPrefix.summaryWindow")).toOption

  def discoverStreams(): Iterable[Either[EventType, Summary]] = {
    // TODO: Support querying for multiples of summaryType and summaryWindow.
    val now = Instant.now().minus(timeOffset)

    val results = eventType match {
      case Some(value) =>
        val entries = value.split(',')
        entries.map { entry =>
          (
            Some(entry),
            esmond.getArchiveList(
              timeStart = Some(now.getEpochSecond - timeRange.getSeconds),
              timeRange = Some(timeRange.getSeconds),
              limit = limit,
              offset = offset,
              source = source,
              destination = destination,
              measurementAgent = measurementAgent,
              toolName = toolName,
              dnsMatchRule = dnsMatchRule,
              eventType = Some(entry),
              summaryType = summaryType,
              summaryWindow = summaryWindow
            )
          )
        }.toList
      case None => Seq(
        (
          None,
          esmond.getArchiveList(
            timeStart = Some(now.getEpochSecond - timeRange.getSeconds),
            timeRange = Some(timeRange.getSeconds),
            limit = limit,
            offset = offset,
            source = source,
            destination = destination,
            measurementAgent = measurementAgent,
            toolName = toolName,
            dnsMatchRule = dnsMatchRule,
            eventType = eventType,
            summaryType = summaryType,
            summaryWindow = summaryWindow
          )
        )
      )
    }

    val checkedForExceptions = results.map { e =>
      e._2 match {
        case Failure(exception) =>
          logger.error(s"Failed discovery call: $exception")
          (e._1, Seq())
        case Success(value) => (e._1, value)
      }
    }

    val events = checkedForExceptions.flatMap {
      case (eTypeOpt, archives) => eTypeOpt match {
        case Some(eType) => archives.flatMap(_.eventTypes.filter(_.eventType == eType))
        case None => archives.flatMap(_.eventTypes)
      }
    }
    val summaries = events.flatMap(_.summaries)
    val eventsAndSummaries = events.map(Left(_)).union(summaries.map(Right(_)))

    // I stole this from https://stackoverflow.com/a/3871593
    def isUniqueList[T](l: Seq[T]): Boolean = isUniqueList1(l, new HashSet[T])

    @tailrec
    def isUniqueList1[T](l: Seq[T], s: Set[T]): Boolean = l match {
      case Nil => true
      case h :: t => if (s(h)) {
        false
      }
      else {
        isUniqueList1(t, s + h)
      }
    }

    if (!isUniqueList(eventsAndSummaries)) {
      logger.error(s"Reported duplicate streams during stream discovery!")
    }

    eventsAndSummaries
  }
}
