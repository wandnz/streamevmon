package nz.net.wand.amp.analyser.measurements

import org.squeryl.annotations.Column

case class TCPPingMeta(
    @Column("stream_id")
    stream: Int,
    source: String,
    destination: String,
    port: Int,
    family: String,
    packet_size: String
) extends MeasurementMeta
