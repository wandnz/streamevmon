package nz.net.wand.amp.analyser.measurements

import org.squeryl.annotations.Column

case class TCPPingMeta(
    @Column("stream_id")
    stream: Int,
    source: String,
    destination: String,
    port: Int,
    family: String,
    @Column("packet_size")
    packet_size_selection: String
) extends MeasurementMeta
