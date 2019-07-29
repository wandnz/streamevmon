package nz.net.wand.amp.analyser.measurements

import org.squeryl.annotations.Column

case class DNSMeta(
    @Column("stream_id")
    stream: Int,
    source: String,
    destination: String,
    instance: String,
    address: String,
    query: String,
    query_type: String,
    query_class: String,
    udp_payload_size: Int,
    recurse: Boolean,
    dnssec: Boolean,
    nsid: Boolean
) extends MeasurementMeta
