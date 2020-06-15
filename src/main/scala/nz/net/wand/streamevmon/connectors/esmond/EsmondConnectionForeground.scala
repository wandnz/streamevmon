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

  private object ResponseType extends Enumeration {
    type ResponseType = Value
    val Failure: Value = Value
    val Histogram: Value = Value
    val Href: Value = Value
    val PacketTrace: Value = Value
    val Simple: Value = Value
    val Subintervals: Value = Value

    def fromString(eventType: String): ResponseType = eventType.toLowerCase match {
      case "failures" => Failure
      case "histogram-ttl" | "histogram-owdelay" => Histogram
      case "pscheduler-run-href" => Href
      case "packet-trace" => PacketTrace
      case "time-error-estimates" |
           "packet-duplicates" |
           "packet-loss-rate" |
           "packet-count-sent" |
           "packet-count-lost" |
           "throughput" |
           "packet-retransmits" |
           "packet-reorders"
      => Simple
      case "throughput-subintervals" | "packet-retransmits-subintervals" => Subintervals
      case "path-mtu" => logger.error("Found path-mtu data!"); Simple
      case other: String => throw new IllegalArgumentException(s"Unknown response type $other")
    }
  }

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

  /** @see [[EsmondAPI.simpleTimeSeries]] */
  def getTimeSeriesEntries(
    metadataKey: String,
    eventType: String,
    timeRange: Option[Long] = None,
    time: Option[Long] = None,
    timeStart: Option[Long] = None,
    timeEnd    : Option[Long] = None,
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

  def getTimeSeriesSummaryEntriesFromMetadata(
    metadataKey: String,
    eventType: String,
    summaryType: String,
    summaryWindow: Long,
    timeRange: Option[Long] = None,
    time: Option[Long] = None,
    timeStart: Option[Long] = None,
    timeEnd      : Option[Long] = None,
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
    val args = (
      summary.metadataKey,
      summary.eventType,
      summary.summaryType,
      summary.summaryWindow.toString,
      timeRange.map(new JLong(_)).orNull,
      time.map(new JLong(_)).orNull,
      timeStart.map(new JLong(_)).orNull,
      timeEnd.map(new JLong(_)).orNull,
    )

    wrapInTrySynchronously(ResponseType.fromString(summary.eventType).toApiCall.tupled(args))
      .asInstanceOf[Try[Iterable[AbstractTimeSeriesEntry]]]
  }
}
