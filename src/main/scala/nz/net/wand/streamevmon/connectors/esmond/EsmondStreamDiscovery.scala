/* This file is part of streamevmon.
 *
 * Copyright (C) 2021  The University of Waikato, Hamilton, New Zealand
 *
 * Author: Daniel Oosterwijk
 *
 * All rights reserved.
 *
 * This code has been developed by the University of Waikato WAND
 * research group. For further information please see https://wand.nz,
 * or our Github organisation at https://github.com/wanduow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
  * This class is configured by the `source.esmond` config key group, which
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
  configPrefix: String = "esmond",
  params      : ParameterTool,
  esmond      : ConnT
) extends AbstractEsmondStreamDiscovery with Logging {
  lazy protected val timeRange: Duration = Duration.ofSeconds(params.getInt(s"source.$configPrefix.discoverTimeRange"))
  lazy protected val timeOffset: Duration = Duration.ofSeconds(params.getInt(s"source.$configPrefix.timeOffset"))
  lazy protected val limit: Option[Long] = Try(params.getLong(s"source.$configPrefix.limit")).toOption
  lazy protected val offset: Option[Long] = Try(params.getLong(s"source.$configPrefix.offset")).toOption
  lazy protected val source: Option[String] = Option(params.get(s"source.$configPrefix.source"))
  lazy protected val destination: Option[String] = Option(params.get(s"source.$configPrefix.destination"))
  lazy protected val measurementAgent: Option[String] = Option(params.get(s"source.$configPrefix.measurementAgent"))
  lazy protected val toolName: Option[String] = Option(params.get(s"source.$configPrefix.toolName"))
  lazy protected val dnsMatchRule: Option[String] = Option(params.get(s"source.$configPrefix.dnsMatchRule"))
  lazy protected val eventType: Option[String] = Option(params.get(s"source.$configPrefix.eventType"))
  lazy protected val summaryType: Option[String] = Option(params.get(s"source.$configPrefix.summaryType"))
  lazy protected val summaryWindow: Option[Long] = Try(params.getLong(s"source.$configPrefix.summaryWindow")).toOption

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
