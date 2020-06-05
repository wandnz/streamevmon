package nz.net.wand.streamevmon.connectors.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema._

import retrofit2.http.{GET, Path, Query}
import retrofit2.Call

/** Describes the supported API calls of this package.
  *
  * Currently, only the time-range query parameter is supported by this interface.
  * However, the API itself supports many more, which are yet to be implemented.
  */
trait EsmondAPI {

  /** Obtains a full list of all the metadata contained in the archive
    * being requested. It is recommended that you specify a reasonable timeRange
    * in order to prevent the request from being too large.
    *
    * @param timeRange The range, in seconds, that data should fall within.
    *                  If timeStart and timeEnd are not provided, the range
    *                  ends at the current time.
    */
  @GET("archive")
  def archiveList(
    @Query("time-range") timeRange: Int
  ): Call[List[Archive]]

  /** Obtains the metadata for a single measurement bundle. This contains some
    * data about the source and destination, the way the test tool is set up,
    * and a bundle of supported event types (`event-types`).
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
