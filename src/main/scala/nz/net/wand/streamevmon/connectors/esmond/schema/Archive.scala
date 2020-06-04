package nz.net.wand.streamevmon.connectors.esmond.schema

import java.io.Serializable

import com.fasterxml.jackson.annotation._
import com.fasterxml.jackson.annotation.JsonInclude.Include

@JsonIgnoreProperties(Array("metadata-previous-page", "metadata-next-page"))
@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder(alphabetic = true)
class Archive extends Serializable {

  // URL
  @JsonProperty("url")
  var url: String = _

  // URL
  // While URL is the full http:// URL, URI is that without the server identifier attached.
  @JsonProperty("uri")
  var uri: String = _

  // UID hex string
  @JsonProperty("metadata-key")
  var metadataKey: String = _

  // "point-to-point"
  @JsonProperty("subject-type")
  var subjectType: String = _

  @JsonProperty("event-types")
  var eventTypes: List[EventType] = List[EventType]()

  // IP
  @JsonProperty("source")
  var source: Option[String] = None

  // IP
  @JsonProperty("destination")
  var destination: Option[String] = None

  // IP
  @JsonProperty("measurement-agent")
  var measurementAgent: Option[String] = None

  // String
  @JsonProperty("tool-name")
  var toolName: Option[String] = None

  // URL
  @JsonProperty("input-source")
  var inputSource: Option[String] = None

  // URL
  @JsonProperty("input-destination")
  var inputDestination: Option[String] = None

  // String
  @JsonProperty("ip-transport-protocol")
  var ipTransportProtocol: Option[String] = None

  // Double in string
  @JsonProperty("sample-bucket-width")
  var sampleBucketWidth: Double = _

  // Sometimes it's an int, sometimes it's "PT5S"
  @JsonProperty("bw-ignore-first-seconds")
  var bwIgnoreFirstSeconds: Option[String] = None

  // Int
  @JsonProperty("metadata-count-total")
  var metadataCountTotal: Int = _

  // Double in string
  @JsonProperty("time-probe-interval")
  var timeProbeInterval: Double = _

  // Int in string
  @JsonProperty("time-duration")
  var timeDuration: Double = _

  // Int in string
  @JsonProperty("time-interval")
  var timeInterval: Int = _

  // Double in string
  @JsonProperty("time-test-timeout")
  var timeTestTimeout: Double = _

  // Int in string
  @JsonProperty("sample-size")
  var sampleSize: Int = _

  // String
  @JsonProperty("pscheduler-test-type")
  var pschedulerTestType: Option[String] = None

  // String
  @JsonProperty("pscheduler-reference-display-set-source")
  var pschedulerReferenceDisplaySetSource: Option[String] = None

  // String
  @JsonProperty("pscheduler-reference-display-set-dest")
  var pschedulerReferenceDisplaySetDest: Option[String] = None

  // String
  @JsonProperty("pscheduler-reference-psconfig-created-by-uuid")
  var pschedulerReferencePsconfigCreatedByUuid: Option[String] = None

  // String
  @JsonProperty("pscheduler-reference-psconfig-created-by-user-agent")
  var pschedulerReferencePsconfigCreatedByUserAgent: Option[String] = None

  // String
  @JsonProperty("pscheduler-reference-created-by-uuid")
  var pschedulerReferenceCreatedByUuid: Option[String] = None

  // String
  @JsonProperty("pscheduler-reference-created-by-user-agent")
  var pschedulerReferenceCreatedByUserAgent: Option[String] = None

  // String
  @JsonProperty("pscheduler-reference-created-by-address")
  var pschedulerReferenceCreatedByAddress: Option[String] = None

  // String
  @JsonProperty("pscheduler-reference-description")
  var pschedulerReferenceDescription: Option[String] = None

  // Boolean in string
  @JsonProperty("mode-flip")
  var modeFlip: Option[Boolean] = None

  // Observed only "0" - this is probably a string for formatting
  @JsonProperty("ip-packet-padding")
  var ipPacketPadding: Option[String] = None

  // Observed only "1", probably Int in string
  @JsonProperty("bw-parallel-streams")
  var BwParallelStreams: Int = _

  // Int in string
  @JsonProperty("tcp-window-size")
  var TcpWindowSize: Int = _
}
