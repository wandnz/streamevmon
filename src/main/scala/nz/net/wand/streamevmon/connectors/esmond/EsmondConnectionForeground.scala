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
      timeRange = timeRange.map(new JLong(_)).orNull,
      time = time.map(new JLong(_)).orNull,
      timeStart = timeStart.map(new JLong(_)).orNull,
      timeEnd = timeEnd.map(new JLong(_)).orNull,
      source = source.orNull,
      destination = destination.orNull,
      measurementAgent = measurementAgent.orNull,
      toolName = toolName.orNull,
      dnsMatchRule = dnsMatchRule.orNull,
      eventType = eventType.orNull,
      summaryType = summaryType.orNull,
      summaryWindow = summaryWindow.map(_.toString).orNull,
      limit = limit.map(new JLong(_)).orNull,
      offset = offset.map(new JLong(_)).orNull
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
      timeRange.map(new JLong(_)).orNull,
      time.map(new JLong(_)).orNull,
      timeStart.map(new JLong(_)).orNull,
      timeEnd.map(new JLong(_)).orNull,
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
      timeRange.map(new JLong(_)).orNull,
      time.map(new JLong(_)).orNull,
      timeStart.map(new JLong(_)).orNull,
      timeEnd.map(new JLong(_)).orNull,
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
