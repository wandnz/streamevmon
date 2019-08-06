package nz.net.wand.amp.analyser.measurements

import org.squeryl.annotations.Column

case class TracerouteMeta(
    @Column("stream_id")
    stream: Int,
    source: String,
    destination: String,
    family: String,
    @Column("packet_size")
    packet_size_selection: String
) extends MeasurementMeta
