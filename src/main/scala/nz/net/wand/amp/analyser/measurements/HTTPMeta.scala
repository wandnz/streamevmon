package nz.net.wand.amp.analyser.measurements

import org.squeryl.annotations.Column

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
