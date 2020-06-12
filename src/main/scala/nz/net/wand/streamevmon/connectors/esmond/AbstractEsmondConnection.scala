package nz.net.wand.streamevmon.connectors.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.{Archive, Summary, TimeSeriesEntry}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import retrofit2.{HttpException, Response, Retrofit}
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.Retrofit.Builder

import scala.util.{Failure, Success, Try}

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
    source          : Option[String] = None,
    destination     : Option[String] = None,
    measurementAgent: Option[String] = None,
    toolName        : Option[String] = None,
    dnsMatchRule    : Option[String] = None,
    eventType       : Option[String] = None
  ): Try[Iterable[Archive]]

  /** @see [[EsmondAPI.archive]]*/
  def getArchive(
    metadataKey: String,
  ): Try[Archive]

  /** @see [[EsmondAPI.timeSeriesBase]]*/
  def getTimeSeriesEntries(
    metadataKey: String,
    eventType  : String,
    timeRange  : Option[Long] = None,
    time       : Option[Long] = None,
    timeStart  : Option[Long] = None,
    timeEnd    : Option[Long] = None,
  ): Try[Iterable[TimeSeriesEntry]]

  /** @see [[EsmondAPI.timeSeriesSummary]]*/
  def getTimeSeriesSummaryEntriesFromMetadata(
    metadataKey  : String,
    eventType    : String,
    summaryType  : String,
    summaryWindow: Long,
    timeRange    : Option[Long] = None,
    time         : Option[Long] = None,
    timeStart    : Option[Long] = None,
    timeEnd      : Option[Long] = None,
  ): Try[Iterable[TimeSeriesEntry]]

  /** @see [[EsmondAPI.timeSeriesSummary]]*/
  def getTimeSeriesSummaryEntries(
    summary  : Summary,
    timeRange: Option[Long] = None,
    time     : Option[Long] = None,
    timeStart: Option[Long] = None,
    timeEnd  : Option[Long] = None,
  ): Try[Iterable[TimeSeriesEntry]]
}
