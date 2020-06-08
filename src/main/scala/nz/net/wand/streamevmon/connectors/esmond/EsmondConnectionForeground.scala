package nz.net.wand.streamevmon.connectors.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema._
import nz.net.wand.streamevmon.Logging

import java.lang.{Long => JLong}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import retrofit2._
import retrofit2.Retrofit.Builder
import retrofit2.converter.jackson.JacksonConverterFactory

import scala.util.{Failure, Success, Try}

/** Acts as an interface for [[EsmondAPI]].
  *
  * @param source The URL of the API host being used as a source. Include the
  *               TLD, but exclude the protocol and port.
  */
case class EsmondConnectionForeground(
  source : String
) extends Logging {
  protected val baseUrl = s"http://$source:8085/esmond/perfsonar/"

  private val retrofit: Retrofit = new Builder()
    .addConverterFactory(JacksonConverterFactory.create(
      new ObjectMapper().registerModule(DefaultScalaModule)
    ))
    .baseUrl(baseUrl)
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

  /** Uses [[wrapInTry]] in the foreground. This method will block until a
    * response is obtained.
    */
  def wrapInTrySynchronously[T](func: Call[T]): Try[T] = {
    logger.info(s"Making request: ${func.request().url().toString}")
    wrapInTry(func.execute)
  }

  /** @see [[EsmondAPI.archiveList]] */
  def getArchiveList(
    timeRange       : Option[Long] = None,
    time            : Option[Long] = None,
    timeStart       : Option[Long] = None,
    timeEnd         : Option[Long] = None,
    source          : Option[String] = None,
    destination     : Option[String] = None,
    measurementAgent: Option[String] = None,
    toolName        : Option[String] = None,
    dnsMatchRule    : Option[String] = None,
    eventType       : Option[String] = None
  ): Try[List[Archive]] = {
    wrapInTrySynchronously(esmondAPI.archiveList(
      timeRange.map(new JLong(_)).orNull,
      time.map(new JLong(_)).orNull,
      timeStart.map(new JLong(_)).orNull,
      timeEnd.map(new JLong(_)).orNull,
      source.orNull,
      destination.orNull,
      measurementAgent.orNull,
      toolName.orNull,
      dnsMatchRule.orNull,
      eventType.orNull,
    ))
  }

  /** @see [[EsmondAPI.archive]] */
  def getArchive(
    metadataKey: String,
    timeRange: Int
  ): Try[Archive] = {
    wrapInTrySynchronously(esmondAPI.archive(metadataKey, timeRange))
  }

  /** @see [[EsmondAPI.timeSeriesBase]] */
  def getTimeSeries(
    metadataKey: String,
    eventType: String,
    timeRange: Int
  ): Try[List[TimeSeriesEntry]] = {
    wrapInTrySynchronously(esmondAPI.timeSeriesBase(metadataKey, eventType, timeRange))
  }

  /** @see [[EsmondAPI.timeSeriesSummary]] */
  def getTimeSeriesSummary(
    metadataKey: String,
    eventType: String,
    summaryType: String,
    summaryWindow: Int,
    timeRange: Int
  ): Try[List[TimeSeriesEntry]] = {
    wrapInTrySynchronously(esmondAPI.timeSeriesSummary(metadataKey, eventType, summaryType, summaryWindow, timeRange))
  }

  /** @see [[EsmondAPI.timeSeriesSummary]] */
  def getTimeSeriesSummary(
    summary: Summary,
    timeRange: Int = 86400
  ): Try[List[TimeSeriesEntry]] = {
    wrapInTrySynchronously(esmondAPI.timeSeriesSummary(
      summary.metadataKey,
      summary.eventType,
      summary.summaryType,
      summary.summaryWindow,
      timeRange
    ))
  }
}
