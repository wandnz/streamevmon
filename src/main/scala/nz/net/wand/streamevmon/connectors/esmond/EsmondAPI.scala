package nz.net.wand.streamevmon.connectors.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.Archive

import retrofit2.http.{GET, Path}
import retrofit2.Call

trait EsmondAPI {

  // This correctly parses the full response containing all the metadata
  // which can be served.
  @GET("archive/")
  def root(): Call[List[Archive]]

  // This is a hard-coded endpoint with an incorrect response type.
  @GET("archive/f6b732e9f351487a96126f0c25e5e546/throughput/base?time-range=86400")
  def specific(): Call[List[String]]

  // This is an incomplete example of creating an endpoint based on previous
  // results.
  @GET("archive/{metadataKey}/{eventType}/base")
  def base(
    @Path("metadataKey") metadataKey: String
  ): Call[List[String]]
}
