package nz.net.wand.amp.analyser.measurements

import org.squeryl.annotations.Column

case class ICMPMeta(
    @Column("stream_id")
    stream: Int,
    source: String,
    destination: String,
    family: String,
    packet_size: String
) extends MeasurementMeta
