package nz.net.wand.amp.analyser.measurements

case class DNSMeta(
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
