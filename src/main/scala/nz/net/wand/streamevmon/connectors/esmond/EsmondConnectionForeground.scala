package nz.net.wand.streamevmon.connectors.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema._
import nz.net.wand.streamevmon.Logging

import java.lang.{Long => JLong}

import retrofit2._

import scala.util.Try

/** Acts as an interface for [[EsmondAPI]].
  *
  * @param source The URL of the API host being used as a source. Include the
  *               TLD, but exclude the protocol and port.
  */
class EsmondConnectionForeground(
  source: String
) extends AbstractEsmondConnection(source)
          with Logging {

  /** Uses [[wrapInTry]] in the foreground. This method will block until a
    * response is obtained.
    */
  def wrapInTrySynchronously[T](func: Call[T]): Try[T] = {
    logger.info(s"Making request: ${func.request().url().toString}")
    wrapInTry(func.execute)
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
  ): Try[Iterable[Archive]] = {
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
  ): Try[Archive] = {
    wrapInTrySynchronously(esmondAPI.archive(metadataKey))
  }

  /** @see [[EsmondAPI.timeSeriesBase]] */
  def getTimeSeriesEntries(
    metadataKey: String,
    eventType  : String,
    timeRange  : Option[Long] = None,
    time       : Option[Long] = None,
    timeStart  : Option[Long] = None,
    timeEnd    : Option[Long] = None,
  ): Try[Iterable[TimeSeriesEntry]] = {
    wrapInTrySynchronously(esmondAPI.timeSeriesBase(
      metadataKey,
      eventType,
      timeRange.map(new JLong(_)).orNull,
      time.map(new JLong(_)).orNull,
      timeStart.map(new JLong(_)).orNull,
      timeEnd.map(new JLong(_)).orNull,
    ))
  }

  /** @see [[EsmondAPI.timeSeriesSummary]] */
  def getTimeSeriesSummaryEntriesFromMetadata(
    metadataKey  : String,
    eventType    : String,
    summaryType  : String,
    summaryWindow: Long,
    timeRange    : Option[Long] = None,
    time         : Option[Long] = None,
    timeStart    : Option[Long] = None,
    timeEnd      : Option[Long] = None,
  ): Try[Iterable[TimeSeriesEntry]] = {
    wrapInTrySynchronously(esmondAPI.timeSeriesSummary(
      metadataKey,
      eventType,
      summaryType,
      summaryWindow,
      timeRange.map(new JLong(_)).orNull,
      time.map(new JLong(_)).orNull,
      timeStart.map(new JLong(_)).orNull,
      timeEnd.map(new JLong(_)).orNull,
    ))
  }

  /** @see [[EsmondAPI.timeSeriesSummary]] */
  def getTimeSeriesSummaryEntries(
    summary  : Summary,
    timeRange: Option[Long] = None,
    time     : Option[Long] = None,
    timeStart: Option[Long] = None,
    timeEnd  : Option[Long] = None,
  ): Try[Iterable[TimeSeriesEntry]] = {
    wrapInTrySynchronously(esmondAPI.timeSeriesSummary(
      summary.metadataKey,
      summary.eventType,
      summary.summaryType,
      summary.summaryWindow,
      timeRange.map(new JLong(_)).orNull,
      time.map(new JLong(_)).orNull,
      timeStart.map(new JLong(_)).orNull,
      timeEnd.map(new JLong(_)).orNull,
    ))
  }
}
