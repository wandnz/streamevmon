package nz.net.wand.streamevmon.measurements.amp

import org.squeryl.annotations.Column

/** Represents the metadata associated with the scheduled test that an AMP DNS
  * measurement is produced from.
  *
  * @see [[HTTP]]
  * @see [[RichHTTP]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-http]]
  */
case class HTTPMeta(
    @Column("stream_id")
    stream: Int,
    source: String,
    destination: String,
    max_connections: Int,
    max_connections_per_server: Int,
    max_persistent_connections_per_server: Int,
    pipelining_max_requests: Int,
    persist: Boolean,
    pipelining: Boolean,
    caching: Boolean
) extends MeasurementMeta
