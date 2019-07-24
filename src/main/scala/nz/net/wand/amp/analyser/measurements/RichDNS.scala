package nz.net.wand.amp.analyser.measurements

import java.time.{Instant, ZoneId}
import java.util.concurrent.TimeUnit

case class RichDNS(
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
    nsid: Boolean,
    flag_aa: Boolean,
    flag_ad: Boolean,
    flag_cd: Boolean,
    flag_qr: Boolean,
    flag_ra: Boolean,
    flag_rd: Boolean,
    flag_tc: Boolean,
    lossrate: Double,
    opcode: Int,
    query_len: Int,
    rcode: Int,
    requests: Int,
    response_size: Int,
    rtt: Int,
    total_additional: Int,
    total_answer: Int,
    total_authority: Int,
    ttl: Int,
    time: Instant
) extends RichMeasurement {

  override def toString: String = {
    s"${DNS.table_name} " +
      s"stream=$stream " +
      s"source=$source " +
      s"destination=$destination " +
      s"instance=$instance " +
      s"address=$address " +
      s"query=$query " +
      s"query_type=$query_type " +
      s"query_class=$query_class " +
      s"udp_payload_size=$udp_payload_size " +
      s"recurse=$recurse " +
      s"dnssec=$dnssec " +
      s"nsid=$nsid " +
      s"flag_aa=$flag_aa " +
      s"flag_ad=$flag_ad " +
      s"flag_cd=$flag_cd " +
      s"flag_qr=$flag_qr " +
      s"flag_ra=$flag_ra " +
      s"flag_rd=$flag_rd " +
      s"flag_tc=$flag_tc " +
      s"lossrate=$lossrate " +
      s"opcode=$opcode " +
      s"query_len=$query_len " +
      s"rcode=$rcode " +
      s"requests=$requests " +
      s"response_size=$response_size " +
      s"rtt=$rtt " +
      s"total_additional=$total_additional " +
      s"total_answer=$total_answer " +
      s"total_authority=$total_authority " +
      s"ttl=$ttl " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }
}

object RichDNS extends RichMeasurementFactory {

  override def create(base: Measurement, meta: MeasurementMeta): Option[RichDNS] = {
    base match {
      case b: DNS =>
        meta match {
          case m: DNSMeta =>
            Some(
              RichDNS(
                m.stream,
                m.source,
                m.destination,
                m.instance,
                m.address,
                m.query,
                m.query_type,
                m.query_class,
                m.udp_payload_size,
                m.recurse,
                m.dnssec,
                m.nsid,
                b.flag_aa,
                b.flag_ad,
                b.flag_cd,
                b.flag_qr,
                b.flag_ra,
                b.flag_rd,
                b.flag_tc,
                b.lossrate,
                b.opcode,
                b.query_len,
                b.rcode,
                b.requests,
                b.response_size,
                b.rtt,
                b.total_additional,
                b.total_answer,
                b.total_authority,
                b.ttl,
                Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(b.time))
              ))
          case _ => None
        }
      case _ => None
    }
  }
}
