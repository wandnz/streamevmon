package nz.net.wand.streamevmon.connectors.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema._

import java.lang.{Long => JLong}

import retrofit2.http.{GET, Path, Query}
import retrofit2.Call

/** Describes the supported API calls of this package.
  *
  * Currently, only the time-range query parameter is supported by this interface.
  * However, the API itself supports many more, which are yet to be implemented.
  */
trait EsmondAPI {

  /** Obtains a full list of all the metadata contained in the archive
    * being requested. It is recommended that you specify a reasonable time
    * range or limit in order to prevent the returned data from being large.
    *
    * @param timeRange        Match only measurements that were updated after the given time (inclusive). If time-end nor time-range is defined, then it will return all results from the start time to the current time. In UNIX timestamp format.
    * @param time             Match a measurement last updated at the exact time given as a UNIX timestamp.
    * @param timeStart        Match only measurements that were updated after the given time (inclusive). If time-end nor time-range is defined, then it will return all results from the start time to the current time. In UNIX timestamp format.
    * @param timeEnd          Match only data that was measured before the given time (inclusive). If time-start nor time-range is provided, then will return all data stored in the archive up to and including the end time. In UNIX timestamp format.
    * @param source           The sender in a point-to-point measurement represented as an IPv4 or IPv6 address. When searching you can provide a DNS name and the server will automatically map it to the correct IP address.
    * @param destination      The receiver in a point-to-point measurement represented as an IPv4 or IPv6 address. When searching you can provide a DNS name and the server will automatically map it to the correct IP address.
    * @param measurementAgent The host where the measurement was initiated represented as an IPv4 or IPv6 address. This may be the source, destination or a third-party host depending on the tool. When searching you can provide a DNS name and the server will automatically map it to the correct IP address.
    * @param toolName         A string indicating the name of the tool that produced the measurement. Examples include bwctl/iperf, bwctl/iperf or powstream.
    * @param dnsMatchRule     Specify how DNS lookups are performed. `v4v6` is default, and uses both. `only-v4`, `only-v6`, `prefer-v4`, and `prefer-v6` are other options.
    * @param eventType        This will return any measurement metadata object that has an item in its event-types list where the event-type field equals the provided value.
    * @param summaryType      Either `average`, `aggregation`, or `statistics`. See [[timeSeriesSummary]].
    * @param summaryWindow    The time range (in seconds) that is summarised.
    * @param limit            The maximum number of results to return. If not set then 1000 results are returned.
    * @param offset           The number of tests to skip. Useful for pagination.
    */
  @GET("archive")
  def archiveList(
    @Query("time-range") timeRange              : JLong,
    @Query("time") time                         : JLong,
    @Query("time-start") timeStart              : JLong,
    @Query("time-end") timeEnd                  : JLong,
    @Query("source") source                     : String = null,
    @Query("destination") destination           : String = null,
    @Query("measurement-agent") measurementAgent: String = null,
    @Query("tool-name") toolName                : String = null,
    @Query("dns-match-rule") dnsMatchRule       : String = null,
    @Query("event-type") eventType              : String = null,
    @Query("summary-type") summaryType          : String = null,
    @Query("summary-window") summaryWindow      : String = null,
    @Query("limit") limit                       : JLong = null,
    @Query("offset") offset                     : JLong = null
  ): Call[List[Archive]]

  /** Obtains the metadata for a single measurement bundle. This contains some
    * data about the source and destination, the way the test tool is set up,
    * and a bundle of supported event types (`event-types`).
    *
    * This actually supports most, if not all of the query parameters supported
    * by [[archiveList]], but most of them are fairly meaningless when doing
    * a lookup on a particular entry, so they are omitted here.
    *
    * @param metadataKey The unique key for the metadata requested.
    * @param timeRange   The range, in seconds, that data should fall within.
    *                    If timeStart and timeEnd are not provided, the range
    *                    ends at the current time.
    */
  @GET("archive/{metadataKey}")
  def archive(
    @Path("metadataKey") metadataKey: String,
    @Query("time-range") timeRange  : Int
  ): Call[Archive]

  /** Obtains a base time series (one that is not aggregated at all). The API
    * appears to give these results ordered by time, but that behaviour is not
    * guaranteed by this layer.
    *
    * @param metadataKey The unique key for the metadata requested.
    * @param eventType   The event type within the metadata bundle referred to by
    *                    metadataKey.
    * @param timeRange   The range, in seconds, that data should fall within.
    *                    If timeStart and timeEnd are not provided, the range
    *                    ends at the current time.
    */
  @GET("archive/{metadataKey}/{eventType}/base")
  def timeSeriesBase(
    @Path("metadataKey") metadataKey: String,
    @Path("eventType") eventType    : String,
    @Query("time-range") timeRange  : Int = 86400
  ): Call[List[TimeSeriesEntry]]

  /** Obtains a time series summary. This might be aggregated in one of a number
    * of ways, specified by the `summaryType` field. The API appears to give
    * these results ordered by time, but that behaviour is not guaranteed by
    * this layer.
    *
    * @param metadataKey   The unique key for the metadata requested.
    * @param eventType     The event type within the metadata bundle referred to by
    *                      metadataKey.
    * @param summaryType   The type of summary, which is one of "averages",
    *                      "aggregations", or "statistics". Note that all of these
    *                      types are pluralised, which is not how they appear in
    *                      the metadata. If you use [[nz.net.wand.streamevmon.connectors.esmond.schema.Summary.summaryType Summary.summaryType]], thise
    *                      is handled for you.
    * @param summaryWindow The time range, in seconds, which is summarised.
    * @param timeRange     The range, in seconds, that data should fall within.
    *                      If timeStart and timeEnd are not provided, the range
    *                      ends at the current time.
    */
  @GET("archive/{metadataKey}/{eventType}/{summaryType}/{summaryWindow}")
  def timeSeriesSummary(
    @Path("metadataKey") metadataKey    : String,
    @Path("eventType") eventType        : String,
    @Path("summaryType") summaryType    : String,
    @Path("summaryWindow") summaryWindow: Int,
    @Query("time-range") timeRange      : Int = 86400
  ): Call[List[TimeSeriesEntry]]
}
