package nz.net.wand.streamevmon.measurements.amp

import org.squeryl.annotations.Column

/** Represents the metadata associated with the scheduled test that an AMP DNS
  * measurement is produced from.
  *
  * @see [[Traceroute]]
  * @see [[RichTraceroute]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-trace]]
  */
case class TracerouteMeta(
  @Column("stream_id")
  stream: Int,
  source  : String,
  destination: String,
  family: String,
  @Column("packet_size")
  packet_size_selection: String
) extends PostgresMeasurementMeta
