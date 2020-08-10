package nz.net.wand.streamevmon.measurements.amp

import org.squeryl.annotations.Column

/** Represents the metadata associated with the scheduled test that an AMP DNS
  * measurement is produced from.
  *
  * @see [[DNS]]
  * @see [[RichDNS]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-dns]]
  */
case class DNSMeta(
  @Column("stream_id")
    stream: Int,
  source  : String,
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
) extends PostgresMeasurementMeta
