package nz.net.wand.streamevmon.connectors.esmond.schema

import nz.net.wand.streamevmon.connectors.esmond.EsmondAPI

import java.io.Serializable

import com.fasterxml.jackson.annotation._
import com.fasterxml.jackson.annotation.JsonInclude.Include

/** Stores a bunch of metadata about a measurement group.
  *
  * @see [[EsmondAPI.archive]]
  */
// These properties are to do with pagination from an archive listing, and are
// not relevant to the actual archive object.
@JsonIgnoreProperties(Array("metadata-count-total", "metadata-previous-page", "metadata-next-page"))
@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder(alphabetic = true)
class Archive extends Serializable {
  // URL
  @JsonProperty("url")
  val url: String = null

  // URL
  // While URL is the full http:// URL, URI is that without the server identifier attached.
  @JsonProperty("uri")
  val uri: String = null

  // UID hex string
  @JsonProperty("metadata-key")
  val metadataKey: String = null

  // "point-to-point"
  @JsonProperty("subject-type")
  val subjectType: String = null

  @JsonProperty("event-types")
  val eventTypes: List[EventType] = List[EventType]()

  // IP
  @JsonProperty("source")
  val source: Option[String] = None

  // IP
  @JsonProperty("destination")
  val destination: Option[String] = None

  // IP
  @JsonProperty("measurement-agent")
  val measurementAgent: Option[String] = None

  // String
  @JsonProperty("tool-name")
  val toolName: Option[String] = None

  // URL
  @JsonProperty("input-source")
  val inputSource: Option[String] = None

  // URL
  @JsonProperty("input-destination")
  val inputDestination: Option[String] = None

  // String
  @JsonProperty("ip-transport-protocol")
  val ipTransportProtocol: Option[String] = None

  // Double in string
  @JsonProperty("sample-bucket-width")
  val sampleBucketWidth: Option[Double] = None

  // Double in string
  @JsonProperty("time-probe-interval")
  val timeProbeInterval: Option[Double] = None

  // Int in string
  @JsonProperty("time-duration")
  val timeDuration: Option[Double] = None

  // Int in string
  @JsonProperty("time-interval")
  val timeInterval: Option[Int] = None

  // Double in string
  @JsonProperty("time-test-timeout")
  val timeTestTimeout: Option[Double] = None

  // Int in string
  @JsonProperty("sample-size")
  val sampleSize: Option[Int] = None

  // String
  @JsonProperty("pscheduler-test-type")
  val pschedulerTestType: Option[String] = None

  // String
  @JsonProperty("pscheduler-reference-display-set-source")
  val pschedulerReferenceDisplaySetSource: Option[String] = None

  // String
  @JsonProperty("pscheduler-reference-display-set-dest")
  val pschedulerReferenceDisplaySetDest: Option[String] = None

  // String
  @JsonProperty("pscheduler-reference-psconfig-created-by-uuid")
  val pschedulerReferencePsconfigCreatedByUuid: Option[String] = None

  // String
  @JsonProperty("pscheduler-reference-psconfig-created-by-user-agent")
  val pschedulerReferencePsconfigCreatedByUserAgent: Option[String] = None

  // String
  @JsonProperty("pscheduler-reference-created-by-uuid")
  val pschedulerReferenceCreatedByUuid: Option[String] = None

  // String
  @JsonProperty("pscheduler-reference-created-by-user-agent")
  val pschedulerReferenceCreatedByUserAgent: Option[String] = None

  // String
  @JsonProperty("pscheduler-reference-created-by-address")
  val pschedulerReferenceCreatedByAddress: Option[String] = None

  // String
  @JsonProperty("pscheduler-reference-description")
  val pschedulerReferenceDescription: Option[String] = None

  // Int in string
  @JsonProperty("trace-first-ttl")
  val traceFirstTtl: Option[Int] = None

  // Boolean in string
  @JsonProperty("mode-flip")
  val modeFlip: Option[Boolean] = None

  // Observed only "0" - this is probably a string for formatting
  @JsonProperty("ip-packet-padding")
  val ipPacketPadding: Option[String] = None

  // Int in string
  @JsonProperty("ip-packet-size")
  val ipPacketSize: Option[Int] = None

  // Observed only "1", probably Int in string
  @JsonProperty("bw-parallel-streams")
  val bwParallelStreams: Option[Int] = None

  // Int in string
  @JsonProperty("bw-target-bandwidth")
  val bwTargetBandwidth: Option[Int] = None

  // Sometimes it's an int, sometimes it's "PT5S"
  @JsonProperty("bw-ignore-first-seconds")
  val bwIgnoreFirstSeconds: Option[String] = None

  // Int in string
  @JsonProperty("tcp-window-size")
  val tcpWindowSize: Option[Int] = None

  def canEqual(other: Any): Boolean = other.isInstanceOf[Archive]

  override def equals(other: Any): Boolean = other match {
    case that: Archive =>
      (that canEqual this) &&
        url == that.url &&
        uri == that.uri &&
        metadataKey == that.metadataKey &&
        subjectType == that.subjectType &&
        eventTypes == that.eventTypes &&
        source == that.source &&
        destination == that.destination &&
        measurementAgent == that.measurementAgent &&
        toolName == that.toolName &&
        inputSource == that.inputSource &&
        inputDestination == that.inputDestination &&
        ipTransportProtocol == that.ipTransportProtocol &&
        sampleBucketWidth == that.sampleBucketWidth &&
        timeProbeInterval == that.timeProbeInterval &&
        timeDuration == that.timeDuration &&
        timeInterval == that.timeInterval &&
        timeTestTimeout == that.timeTestTimeout &&
        sampleSize == that.sampleSize &&
        pschedulerTestType == that.pschedulerTestType &&
        pschedulerReferenceDisplaySetSource == that.pschedulerReferenceDisplaySetSource &&
        pschedulerReferenceDisplaySetDest == that.pschedulerReferenceDisplaySetDest &&
        pschedulerReferencePsconfigCreatedByUuid == that.pschedulerReferencePsconfigCreatedByUuid &&
        pschedulerReferencePsconfigCreatedByUserAgent == that.pschedulerReferencePsconfigCreatedByUserAgent &&
        pschedulerReferenceCreatedByUuid == that.pschedulerReferenceCreatedByUuid &&
        pschedulerReferenceCreatedByUserAgent == that.pschedulerReferenceCreatedByUserAgent &&
        pschedulerReferenceCreatedByAddress == that.pschedulerReferenceCreatedByAddress &&
        pschedulerReferenceDescription == that.pschedulerReferenceDescription &&
        traceFirstTtl == that.traceFirstTtl &&
        modeFlip == that.modeFlip &&
        ipPacketPadding == that.ipPacketPadding &&
        ipPacketSize == that.ipPacketSize &&
        bwParallelStreams == that.bwParallelStreams &&
        bwTargetBandwidth == that.bwTargetBandwidth &&
        bwIgnoreFirstSeconds == that.bwIgnoreFirstSeconds &&
        tcpWindowSize == that.tcpWindowSize
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(url, uri, metadataKey, subjectType, eventTypes, source, destination, measurementAgent, toolName, inputSource, inputDestination, ipTransportProtocol, sampleBucketWidth, timeProbeInterval, timeDuration, timeInterval, timeTestTimeout, sampleSize, pschedulerTestType, pschedulerReferenceDisplaySetSource, pschedulerReferenceDisplaySetDest, pschedulerReferencePsconfigCreatedByUuid, pschedulerReferencePsconfigCreatedByUserAgent, pschedulerReferenceCreatedByUuid, pschedulerReferenceCreatedByUserAgent, pschedulerReferenceCreatedByAddress, pschedulerReferenceDescription, traceFirstTtl, modeFlip, ipPacketPadding, ipPacketSize, bwParallelStreams, bwTargetBandwidth, bwIgnoreFirstSeconds, tcpWindowSize)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
