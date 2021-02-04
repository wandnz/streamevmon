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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import retrofit2.{HttpException, Response, Retrofit}
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.Retrofit.Builder

import scala.util.{Failure, Success, Try}

/** A generic interface for [[EsmondAPI]].
  *
  * @param source The URL of the API host being used as a source. Include the
  *               TLD, protocol, and port, but not the API path.
  */
abstract class AbstractEsmondConnection(
  source: String
) {
  protected val baseUrl = s"$source/esmond/perfsonar/"

  private val retrofit: Retrofit = new Builder()
    .addConverterFactory(JacksonConverterFactory.create(
      new ObjectMapper().registerModule(DefaultScalaModule)
    ))
    .baseUrl(baseUrl)
    // Yikes, hosts using https all have self-signed certificates. Scary.
    .client(UnsafeOkHttpClient.get())
    .build()

  protected val esmondAPI: EsmondAPI = retrofit.create(classOf[EsmondAPI])

  /** Handles failures for API functions, wrapping them in a Try object.
    *
    * @param func The API function to call.
    * @tparam T The type that the function returns.
    *
    * @return A Success[T] if the response contained a proper body, or else
    *         a Failure. The Failure can contain either an HttpException if the
    *         server responded with a failure-type HTTP response code, or a
    *         Throwable of some other type if the request didn't work at all.
    */
  protected def wrapInTry[T](func: () => Response[T]): Try[T] = {
    try {
      val response = func()
      if (response.isSuccessful) {
        Success(response.body())
      }
      else {
        Failure(new HttpException(response))
      }
    }
    catch {
      case e: Throwable => Failure(e)
    }
  }

  /** @see [[EsmondAPI.archiveList]]*/
  def getArchiveList(
    timeRange       : Option[Long] = None,
    time            : Option[Long] = None,
    timeStart       : Option[Long] = None,
    timeEnd         : Option[Long] = None,
    limit           : Option[Long] = None,
    offset          : Option[Long] = None,
    source          : Option[String] = None,
    destination     : Option[String] = None,
    measurementAgent: Option[String] = None,
    toolName        : Option[String] = None,
    dnsMatchRule    : Option[String] = None,
    eventType       : Option[String] = None,
    summaryType     : Option[String] = None,
    summaryWindow   : Option[Long] = None,
  ): Try[Iterable[Archive]]

  /** @see [[EsmondAPI.archive]] */
  def getArchive(
    metadataKey: String,
  ): Try[Archive]

  /** Gets a base (non-summarised) time series response.
    *
    * @see [[EsmondAPI.failureTimeSeries]]
    * @see [[EsmondAPI.histogramTimeSeries]]
    * @see [[EsmondAPI.hrefTimeSeries]]
    * @see [[EsmondAPI.packetTraceTimeSeries]]
    * @see [[EsmondAPI.simpleTimeSeries]]
    * @see [[EsmondAPI.subintervalTimeSeries]]
    */
  def getTimeSeriesEntriesFromMetadata(
    metadataKey: String,
    eventType  : String,
    timeRange  : Option[Long] = None,
    time       : Option[Long] = None,
    timeStart  : Option[Long] = None,
    timeEnd    : Option[Long] = None,
  ): Try[Iterable[AbstractTimeSeriesEntry]]

  /** Gets a base (non-summarised) time series response from an
    * [[nz.net.wand.streamevmon.connectors.esmond.schema.EventType EventType]] object.
    *
    * @see [[EsmondAPI.failureTimeSeries]]
    * @see [[EsmondAPI.histogramTimeSeries]]
    * @see [[EsmondAPI.hrefTimeSeries]]
    * @see [[EsmondAPI.packetTraceTimeSeries]]
    * @see [[EsmondAPI.simpleTimeSeries]]
    * @see [[EsmondAPI.subintervalTimeSeries]]
    */
  def getTimeSeriesEntries(
    eventType: EventType,
    timeRange: Option[Long] = None,
    time     : Option[Long] = None,
    timeStart: Option[Long] = None,
    timeEnd  : Option[Long] = None,
  ): Try[Iterable[AbstractTimeSeriesEntry]]

  /** Gets a summarised time series response.
    *
    * @see [[EsmondAPI.failureTimeSeries]]
    * @see [[EsmondAPI.histogramTimeSeries]]
    * @see [[EsmondAPI.hrefTimeSeries]]
    * @see [[EsmondAPI.packetTraceTimeSeries]]
    * @see [[EsmondAPI.simpleTimeSeries]]
    * @see [[EsmondAPI.subintervalTimeSeries]]
    */
  def getTimeSeriesSummaryEntriesFromMetadata(
    metadataKey  : String,
    eventType    : String,
    summaryType  : String,
    summaryWindow: Long,
    timeRange    : Option[Long] = None,
    time         : Option[Long] = None,
    timeStart    : Option[Long] = None,
    timeEnd      : Option[Long] = None,
  ): Try[Iterable[AbstractTimeSeriesEntry]]

  /** Gets a summarised time series response from a
    * [[nz.net.wand.streamevmon.connectors.esmond.schema.Summary Summary]] object.
    *
    * @see [[EsmondAPI.failureTimeSeries]]
    * @see [[EsmondAPI.histogramTimeSeries]]
    * @see [[EsmondAPI.hrefTimeSeries]]
    * @see [[EsmondAPI.packetTraceTimeSeries]]
    * @see [[EsmondAPI.simpleTimeSeries]]
    * @see [[EsmondAPI.subintervalTimeSeries]]
    */
  def getTimeSeriesSummaryEntries(
    summary  : Summary,
    timeRange: Option[Long] = None,
    time     : Option[Long] = None,
    timeStart: Option[Long] = None,
    timeEnd  : Option[Long] = None,
  ): Try[Iterable[AbstractTimeSeriesEntry]]
}
