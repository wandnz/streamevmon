package nz.net.wand.amp.analyser.measurements

import org.squeryl.annotations.Column

/** Represents the metadata associated with the scheduled test that an AMP DNS
  * measurement is produced from.
  *
  * @see [[ICMP]]
  * @see [[RichICMP]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-icmp]]
  */
case class ICMPMeta(
    @Column("stream_id")
    stream: Int,
    source: String,
    destination: String,
    family: String,
    @Column("packet_size")
    packet_size_selection: String
) extends MeasurementMeta
