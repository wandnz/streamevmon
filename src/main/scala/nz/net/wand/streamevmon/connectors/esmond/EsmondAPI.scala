package nz.net.wand.streamevmon.connectors.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema._

import java.lang.{Long => JLong}
import java.time._

import retrofit2.http.{GET, Path, Query}
import retrofit2.Call

/** Describes the supported API calls of this package. */
trait EsmondAPI {

  /** Obtains a full list of all the metadata archives offered by the API
    * endpoint. It is recommended that you specify a reasonable time
    * range or limit, otherwise you risk a very large response where most
    * of the results are irrelevant or potentially invalid.
    *
    * @param timeRange        Match only measurements that were updated after the given time (inclusive). If time-end nor time-range is defined, then it will return all results from the start time to the current time. In UNIX timestamp format.
    * @param time             Match a measurement last updated at the exact time given as a UNIX timestamp.
    * @param timeStart        Match only measurements that were updated after the given time (inclusive). If time-end nor time-range is defined, then it will return all results from the start time to the current time. In UNIX timestamp format.
    * @param timeEnd          Match only data that was measured before the given time (inclusive). If time-start nor time-range is provided, then will return all data stored in the archive up to and including the end time. In UNIX timestamp format.
    * @param limit            The maximum number of results to return. If not set then 1000 results are returned.
    * @param offset           The number of tests to skip. Useful for pagination.
    * @param source           The sender in a point-to-point measurement represented as an IPv4 or IPv6 address. When searching you can provide a DNS name and the server will automatically map it to the correct IP address.
    * @param destination      The receiver in a point-to-point measurement represented as an IPv4 or IPv6 address. When searching you can provide a DNS name and the server will automatically map it to the correct IP address.
    * @param measurementAgent The host where the measurement was initiated represented as an IPv4 or IPv6 address. This may be the source, destination or a third-party host depending on the tool. When searching you can provide a DNS name and the server will automatically map it to the correct IP address.
    * @param toolName         A string indicating the name of the tool that produced the measurement. Examples include bwctl/iperf, bwctl/iperf or powstream.
    * @param dnsMatchRule     Specify how DNS lookups are performed. `v4v6` is default, and uses both. `only-v4`, `only-v6`, `prefer-v4`, and `prefer-v6` are other options.
    * @param eventType        This will return any measurement metadata object that has an item in its event-types list where the event-type field equals the provided value.
    * @param summaryType      Either `average`, `aggregation`, or `statistics`.
    * @param summaryWindow    The time range (in seconds) that is summarised.
    */
  @GET("archive")
  def archiveList(
    @Query("time-range") timeRange              : JLong = null,
    @Query("time") time                         : JLong = null,
    @Query("time-start") timeStart              : JLong = null,
    @Query("time-end") timeEnd                  : JLong = null,
    @Query("limit") limit                       : JLong = null,
    @Query("offset") offset                     : JLong = null,
    @Query("source") source                     : String = null,
    @Query("destination") destination           : String = null,
    @Query("measurement-agent") measurementAgent: String = null,
    @Query("tool-name") toolName                : String = null,
    @Query("dns-match-rule") dnsMatchRule       : String = null,
    @Query("event-type") eventType              : String = null,
    @Query("summary-type") summaryType          : String = null,
    @Query("summary-window") summaryWindow      : String = null,
  ): Call[Iterable[Archive]]

  /** Obtains the metadata for a single measurement bundle. This contains some
    * data about the source and destination, the way the test tool is set up,
    * and a list of supported event types (`event-types`), which may or may not
    * each contain a number of summaries.
    *
    * This actually supports most, if not all of the query parameters supported
    * by [[archiveList]], but most of them are fairly meaningless when doing
    * a lookup on a particular entry, so they are omitted here.
    *
    * @param metadataKey The unique key for the metadata requested.
    */
  @GET("archive/{metadataKey}")
  def archive(
    @Path("metadataKey") metadataKey: String
  ): Call[Archive]

  /** Obtains a simple time series entry, where the value type is just a Double.
    *
    * Supports both base and summarised time series.
    *
    * @param metadataKey   The unique key for the metadata requested.
    * @param eventType     The event type within the metadata bundle referred to by metadataKey.
    * @param summaryType   Either `base`, `average`, `aggregation`, or `statistics`. `base` represents a non-summarised time series.
    * @param summaryWindow The time range (in seconds) that is summarised. Use "" for a base time series.
    * @param timeRange     Match only measurements that were updated after the given time (inclusive). If time-end nor time-range is defined, then it will return all results from the start time to the current time. In UNIX timestamp format.
    * @param time          Match a measurement last updated at the exact time given as a UNIX timestamp.
    * @param timeStart     Match only measurements that were updated after the given time (inclusive). If time-end nor time-range is defined, then it will return all results from the start time to the current time. In UNIX timestamp format.
    * @param timeEnd       Match only data that was measured before the given time (inclusive). If time-start nor time-range is provided, then will return all data stored in the archive up to and including the end time. In UNIX timestamp format.
    */
  @GET("archive/{metadataKey}/{eventType}/{summaryType}/{summaryWindow}")
  def simpleTimeSeries(
    @Path("metadataKey") metadataKey    : String,
    @Path("eventType") eventType        : String,
    @Path("summaryType") summaryType    : String = "base",
    @Path("summaryWindow") summaryWindow: String = "",
    @Query("time-range") timeRange      : JLong = null,
    @Query("time") time                 : JLong = null,
    @Query("time-start") timeStart      : JLong = null,
    @Query("time-end") timeEnd          : JLong = null,
  ): Call[Iterable[SimpleTimeSeriesEntry]]

  /** Obtains a histogram series entry.
    *
    * Supports both base and summarised time series.
    *
    * @param metadataKey   The unique key for the metadata requested.
    * @param eventType     The event type within the metadata bundle referred to by metadataKey.
    * @param summaryType   Either `base`, `average`, `aggregation`, or `statistics`. `base` represents a non-summarised time series.
    * @param summaryWindow The time range (in seconds) that is summarised. Use "" for a base time series.
    * @param timeRange     Match only measurements that were updated after the given time (inclusive). If time-end nor time-range is defined, then it will return all results from the start time to the current time. In UNIX timestamp format.
    * @param time          Match a measurement last updated at the exact time given as a UNIX timestamp.
    * @param timeStart     Match only measurements that were updated after the given time (inclusive). If time-end nor time-range is defined, then it will return all results from the start time to the current time. In UNIX timestamp format.
    * @param timeEnd       Match only data that was measured before the given time (inclusive). If time-start nor time-range is provided, then will return all data stored in the archive up to and including the end time. In UNIX timestamp format.
    */
  @GET("archive/{metadataKey}/{eventType}/{summaryType}/{summaryWindow}")
  def histogramTimeSeries(
    @Path("metadataKey") metadataKey    : String,
    @Path("eventType") eventType        : String,
    @Path("summaryType") summaryType    : String = "base",
    @Path("summaryWindow") summaryWindow: String = "",
    @Query("time-range") timeRange      : JLong = null,
    @Query("time") time                 : JLong = null,
    @Query("time-start") timeStart      : JLong = null,
    @Query("time-end") timeEnd          : JLong = null,
  ): Call[Iterable[HistogramTimeSeriesEntry]]

  /** Obtains an href series entry, where the value type is a link to a more detailed result.
    * These will need more work to get the detailed results, if needed.
    *
    * Supports both base and summarised time series.
    *
    * @param metadataKey   The unique key for the metadata requested.
    * @param eventType     The event type within the metadata bundle referred to by metadataKey.
    * @param summaryType   Either `base`, `average`, `aggregation`, or `statistics`. `base` represents a non-summarised time series.
    * @param summaryWindow The time range (in seconds) that is summarised. Use "" for a base time series.
    * @param timeRange     Match only measurements that were updated after the given time (inclusive). If time-end nor time-range is defined, then it will return all results from the start time to the current time. In UNIX timestamp format.
    * @param time          Match a measurement last updated at the exact time given as a UNIX timestamp.
    * @param timeStart     Match only measurements that were updated after the given time (inclusive). If time-end nor time-range is defined, then it will return all results from the start time to the current time. In UNIX timestamp format.
    * @param timeEnd       Match only data that was measured before the given time (inclusive). If time-start nor time-range is provided, then will return all data stored in the archive up to and including the end time. In UNIX timestamp format.
    */
  @GET("archive/{metadataKey}/{eventType}/{summaryType}/{summaryWindow}")
  def hrefTimeSeries(
    @Path("metadataKey") metadataKey    : String,
    @Path("eventType") eventType        : String,
    @Path("summaryType") summaryType    : String = "base",
    @Path("summaryWindow") summaryWindow: String = "",
    @Query("time-range") timeRange      : JLong = null,
    @Query("time") time                 : JLong = null,
    @Query("time-start") timeStart      : JLong = null,
    @Query("time-end") timeEnd          : JLong = null,
  ): Call[Iterable[HrefTimeSeriesEntry]]

  /** Obtains a subinterval time series entry, where the value type is a list of
    * time intervals, each of which has a value and a potentially different size.
    *
    * Supports both base and summarised time series.
    *
    * @param metadataKey   The unique key for the metadata requested.
    * @param eventType     The event type within the metadata bundle referred to by metadataKey.
    * @param summaryType   Either `base`, `average`, `aggregation`, or `statistics`. `base` represents a non-summarised time series.
    * @param summaryWindow The time range (in seconds) that is summarised. Use "" for a base time series.
    * @param timeRange     Match only measurements that were updated after the given time (inclusive). If time-end nor time-range is defined, then it will return all results from the start time to the current time. In UNIX timestamp format.
    * @param time          Match a measurement last updated at the exact time given as a UNIX timestamp.
    * @param timeStart     Match only measurements that were updated after the given time (inclusive). If time-end nor time-range is defined, then it will return all results from the start time to the current time. In UNIX timestamp format.
    * @param timeEnd       Match only data that was measured before the given time (inclusive). If time-start nor time-range is provided, then will return all data stored in the archive up to and including the end time. In UNIX timestamp format.
    */
  @GET("archive/{metadataKey}/{eventType}/{summaryType}/{summaryWindow}")
  def subintervalTimeSeries(
    @Path("metadataKey") metadataKey    : String,
    @Path("eventType") eventType        : String,
    @Path("summaryType") summaryType    : String = "base",
    @Path("summaryWindow") summaryWindow: String = "",
    @Query("time-range") timeRange      : JLong = null,
    @Query("time") time                 : JLong = null,
    @Query("time-start") timeStart      : JLong = null,
    @Query("time-end") timeEnd          : JLong = null,
  ): Call[Iterable[SubintervalTimeSeriesEntry]]

  /** Obtains a failure time series entry, which attaches a timestamp to errors in the measurement process.
    *
    * Supports both base and summarised time series.
    *
    * @param metadataKey   The unique key for the metadata requested.
    * @param eventType     The event type within the metadata bundle referred to by metadataKey.
    * @param summaryType   Either `base`, `average`, `aggregation`, or `statistics`. `base` represents a non-summarised time series.
    * @param summaryWindow The time range (in seconds) that is summarised. Use "" for a base time series.
    * @param timeRange     Match only measurements that were updated after the given time (inclusive). If time-end nor time-range is defined, then it will return all results from the start time to the current time. In UNIX timestamp format.
    * @param time          Match a measurement last updated at the exact time given as a UNIX timestamp.
    * @param timeStart     Match only measurements that were updated after the given time (inclusive). If time-end nor time-range is defined, then it will return all results from the start time to the current time. In UNIX timestamp format.
    * @param timeEnd       Match only data that was measured before the given time (inclusive). If time-start nor time-range is provided, then will return all data stored in the archive up to and including the end time. In UNIX timestamp format.
    */
  @GET("archive/{metadataKey}/{eventType}/{summaryType}/{summaryWindow}")
  def failureTimeSeries(
    @Path("metadataKey") metadataKey    : String,
    @Path("eventType") eventType        : String,
    @Path("summaryType") summaryType    : String = "base",
    @Path("summaryWindow") summaryWindow: String = "",
    @Query("time-range") timeRange      : JLong = null,
    @Query("time") time                 : JLong = null,
    @Query("time-start") timeStart      : JLong = null,
    @Query("time-end") timeEnd          : JLong = null,
  ): Call[Iterable[FailureTimeSeriesEntry]]

  /** Obtains a packet trace time series entry, where the value represents the path a packet took from one host to another.
    *
    * Supports both base and summarised time series.
    *
    * @param metadataKey   The unique key for the metadata requested.
    * @param eventType     The event type within the metadata bundle referred to by metadataKey.
    * @param summaryType   Either `base`, `average`, `aggregation`, or `statistics`. `base` represents a non-summarised time series.
    * @param summaryWindow The time range (in seconds) that is summarised. Use "" for a base time series.
    * @param timeRange     Match only measurements that were updated after the given time (inclusive). If time-end nor time-range is defined, then it will return all results from the start time to the current time. In UNIX timestamp format.
    * @param time          Match a measurement last updated at the exact time given as a UNIX timestamp.
    * @param timeStart     Match only measurements that were updated after the given time (inclusive). If time-end nor time-range is defined, then it will return all results from the start time to the current time. In UNIX timestamp format.
    * @param timeEnd       Match only data that was measured before the given time (inclusive). If time-start nor time-range is provided, then will return all data stored in the archive up to and including the end time. In UNIX timestamp format.
    */
  @GET("archive/{metadataKey}/{eventType}/{summaryType}/{summaryWindow}")
  def packetTraceTimeSeries(
    @Path("metadataKey") metadataKey    : String,
    @Path("eventType") eventType        : String,
    @Path("summaryType") summaryType    : String = "base",
    @Path("summaryWindow") summaryWindow: String = "",
    @Query("time-range") timeRange      : JLong = null,
    @Query("time") time                 : JLong = null,
    @Query("time-start") timeStart      : JLong = null,
    @Query("time-end") timeEnd          : JLong = null,
  ): Call[Iterable[PacketTraceTimeSeriesEntry]]
}

object EsmondAPI {

  /** We can query netbeam in terms of tiles, which when in the format '1d-xxxxx'
    * represent the number of days since the epoch, in UTC. Esmond doesn't
    * support this, so let's provide a helper function to turn them into a pair
    * of Instants which can be converted back into a timeStart and timeEnd that
    * esmond understands.
    */
  def tileToTimeRange(tile: Int): (Instant, Instant) = {
    (
      LocalDate.ofEpochDay(tile).atStartOfDay().toInstant(ZoneOffset.UTC),
      LocalDate.ofEpochDay(tile).atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC)
    )
  }

  def tileToTimeRange(tile: String): (Instant, Instant) = {
    if (tile.startsWith("1d-")) {
      tileToTimeRange(tile.split('-')(1).toInt)
    }
    else {
      throw new IllegalArgumentException(s"Expected tile string to start with '1d-', got $tile")
    }
  }
}
