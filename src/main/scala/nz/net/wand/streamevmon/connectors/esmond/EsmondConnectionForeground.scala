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

import nz.net.wand.streamevmon.connectors.esmond.schema._
import nz.net.wand.streamevmon.Logging

import java.lang.{Long => JLong}

import retrofit2._

import scala.util.Try

/** Acts as an interface for [[EsmondAPI]]. Does all API calls in the current
  * thread.
  *
  * @param source The URL of the API host being used as a source. Include the
  *               TLD, protocol, and port.
  */
class EsmondConnectionForeground(
  source: String
) extends AbstractEsmondConnection(source)
          with Logging {

  /** When combined with [[ResponseType]], this lets us map eventType strings
    * into the API call returning the correct type.
    */
  private implicit class ExtensionMethod(responseType: ResponseType.Value) {
    type ApiCall = (String, String, String, String, JLong, JLong, JLong, JLong) => Call[Iterable[AbstractTimeSeriesEntry]]

    def toApiCall: ApiCall = {
      responseType match {
        case ResponseType.Failure => (esmondAPI.failureTimeSeries _).asInstanceOf[ApiCall]
        case ResponseType.Histogram => (esmondAPI.histogramTimeSeries _).asInstanceOf[ApiCall]
        case ResponseType.Href => (esmondAPI.hrefTimeSeries _).asInstanceOf[ApiCall]
        case ResponseType.PacketTrace => (esmondAPI.packetTraceTimeSeries _).asInstanceOf[ApiCall]
        case ResponseType.Simple => (esmondAPI.simpleTimeSeries _).asInstanceOf[ApiCall]
        case ResponseType.Subintervals => (esmondAPI.subintervalTimeSeries _).asInstanceOf[ApiCall]
      }
    }
  }

  /** Uses [[wrapInTry]] in the foreground. This method will block until a
    * response is obtained.
    */
  def wrapInTrySynchronously[T](func: Call[T]): Try[T] = {
    logger.info(s"Making request: ${func.request().url().toString}")
    wrapInTry(func.execute)
  }

  def getArchiveList(
    timeRange: Option[Long] = None,
    time: Option[Long] = None,
    timeStart: Option[Long] = None,
    timeEnd: Option[Long] = None,
    limit: Option[Long] = None,
    offset: Option[Long] = None,
    source: Option[String] = None,
    destination: Option[String] = None,
    measurementAgent: Option[String] = None,
    toolName: Option[String] = None,
    dnsMatchRule: Option[String] = None,
    eventType: Option[String] = None,
    summaryType: Option[String] = None,
    summaryWindow: Option[Long] = None,
  ): Try[Iterable[Archive]] = {
    wrapInTrySynchronously(esmondAPI.archiveList(
      timeRange = timeRange.map(JLong.valueOf).orNull,
      time = time.map(JLong.valueOf).orNull,
      timeStart = timeStart.map(JLong.valueOf).orNull,
      timeEnd = timeEnd.map(JLong.valueOf).orNull,
      source = source.orNull,
      destination = destination.orNull,
      measurementAgent = measurementAgent.orNull,
      toolName = toolName.orNull,
      dnsMatchRule = dnsMatchRule.orNull,
      eventType = eventType.orNull,
      summaryType = summaryType.orNull,
      summaryWindow = summaryWindow.map(_.toString).orNull,
      limit = limit.map(JLong.valueOf).orNull,
      offset = offset.map(JLong.valueOf).orNull
    ))
  }

  def getArchive(
    metadataKey: String,
  ): Try[Archive] = {
    wrapInTrySynchronously(esmondAPI.archive(metadataKey))
  }

  def getTimeSeriesEntriesFromMetadata(
    metadataKey: String,
    eventType: String,
    timeRange: Option[Long] = None,
    time: Option[Long] = None,
    timeStart: Option[Long] = None,
    timeEnd: Option[Long] = None,
  ): Try[Iterable[AbstractTimeSeriesEntry]] = {
    val args = (
      metadataKey,
      eventType,
      "base",
      "",
      timeRange.map(JLong.valueOf).orNull,
      time.map(JLong.valueOf).orNull,
      timeStart.map(JLong.valueOf).orNull,
      timeEnd.map(JLong.valueOf).orNull,
    )

    wrapInTrySynchronously(ResponseType.fromString(eventType).toApiCall.tupled(args))
      .asInstanceOf[Try[Iterable[AbstractTimeSeriesEntry]]]
  }

  def getTimeSeriesEntries(
    eventType: EventType,
    timeRange: Option[Long] = None,
    time: Option[Long] = None,
    timeStart: Option[Long] = None,
    timeEnd: Option[Long] = None,
  ): Try[Iterable[AbstractTimeSeriesEntry]] = {
    getTimeSeriesEntriesFromMetadata(
      eventType.metadataKey,
      eventType.eventType,
      timeRange,
      time,
      timeStart,
      timeEnd
    )
  }

  def getTimeSeriesSummaryEntriesFromMetadata(
    metadataKey: String,
    eventType  : String,
    summaryType: String,
    summaryWindow: Long,
    timeRange: Option[Long] = None,
    time: Option[Long] = None,
    timeStart: Option[Long] = None,
    timeEnd: Option[Long] = None,
  ): Try[Iterable[AbstractTimeSeriesEntry]] = {
    val args = (
      metadataKey,
      eventType,
      summaryType,
      summaryWindow.toString,
      timeRange.map(JLong.valueOf).orNull,
      time.map(JLong.valueOf).orNull,
      timeStart.map(JLong.valueOf).orNull,
      timeEnd.map(JLong.valueOf).orNull,
    )

    wrapInTrySynchronously(ResponseType.fromString(eventType).toApiCall.tupled(args))
      .asInstanceOf[Try[Iterable[AbstractTimeSeriesEntry]]]
  }

  def getTimeSeriesSummaryEntries(
    summary: Summary,
    timeRange: Option[Long] = None,
    time: Option[Long] = None,
    timeStart: Option[Long] = None,
    timeEnd  : Option[Long] = None,
  ): Try[Iterable[AbstractTimeSeriesEntry]] = {
    getTimeSeriesSummaryEntriesFromMetadata(
      summary.metadataKey,
      summary.eventType,
      summary.summaryType,
      summary.summaryWindow,
      timeRange,
      time,
      timeStart,
      timeEnd
    )
  }
}
