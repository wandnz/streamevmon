package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.measurements.traits.PostgresMeasurementMeta

import org.squeryl.annotations.Column

/** Represents the metadata associated with the scheduled test that an AMP
  * Traceroute or Traceroute-Pathlen measurement is produced from.
  *
  * @see [[TraceroutePathlen]]
  * @see [[RichTraceroutePathlen]]
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
