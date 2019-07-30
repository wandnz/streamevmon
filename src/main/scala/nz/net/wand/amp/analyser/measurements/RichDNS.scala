package nz.net.wand.amp.analyser.measurements

import java.time.{Instant, ZoneId}

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
    flag_aa: Option[Boolean],
    flag_ad: Option[Boolean],
    flag_cd: Option[Boolean],
    flag_qr: Option[Boolean],
    flag_ra: Option[Boolean],
    flag_rd: Option[Boolean],
    flag_tc: Option[Boolean],
    lossrate: Double,
    opcode: Option[Int],
    query_len: Int,
    rcode: Option[Int],
    requests: Int,
    response_size: Option[Int],
    rtt: Option[Int],
    total_additional: Option[Int],
    total_answer: Option[Int],
    total_authority: Option[Int],
    ttl: Option[Int],
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
      s"flag_aa=${flag_aa.get}," +
      s"flag_ad=${flag_ad.get}," +
      s"flag_cd=${flag_cd.get}," +
      s"flag_qr=${flag_qr.get}," +
      s"flag_ra=${flag_ra.get}," +
      s"flag_rd=${flag_rd.get}," +
      s"flag_tc=${flag_tc.get}," +
      s"lossrate=$lossrate," +
      s"opcode=${opcode.get}," +
      s"query_len=$query_len," +
      s"rcode=${rcode.get}," +
      s"requests=$requests," +
      s"response_size=${response_size.get}," +
      s"rtt=${rtt.get}," +
      s"total_additional=${total_additional.get}," +
      s"total_answer=${total_answer.get}," +
      s"total_authority=${total_authority.get}," +
      s"ttl=${ttl.get} " +
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
                b.time
              ))
          case _ => None
        }
      case _ => None
    }
  }
}
