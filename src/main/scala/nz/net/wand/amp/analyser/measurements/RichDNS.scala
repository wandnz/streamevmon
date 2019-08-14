package nz.net.wand.amp.analyser.measurements

import java.time.{Instant, ZoneId}

/** Represents an AMP DNS measurement, as well as the metadata associated with
  * the scheduled test that generated it.
  *
  * @see [[DNS]]
  * @see [[DNSMeta]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-dns]]
  */
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
      s"source=$source," +
      s"destination=$destination," +
      s"instance=$instance," +
      s"address=$address," +
      s"query=$query," +
      s"query_type=$query_type," +
      s"query_class=$query_class," +
      s"udp_payload_size=$udp_payload_size," +
      s"recurse=$recurse," +
      s"dnssec=$dnssec," +
      s"nsid=$nsid," +
      s"flag_aa=${flag_aa.getOrElse("")}," +
      s"flag_ad=${flag_ad.getOrElse("")}," +
      s"flag_cd=${flag_cd.getOrElse("")}," +
      s"flag_qr=${flag_qr.getOrElse("")}," +
      s"flag_ra=${flag_ra.getOrElse("")}," +
      s"flag_rd=${flag_rd.getOrElse("")}," +
      s"flag_tc=${flag_tc.getOrElse("")}," +
      s"lossrate=$lossrate," +
      s"opcode=${opcode.map(x => s"${x}i").getOrElse("")}," +
      s"query_len=${query_len}i," +
      s"rcode=${rcode.map(x => s"${x}i").getOrElse("")}," +
      s"requests=${requests}i," +
      s"response_size=${response_size.map(x => s"${x}i").getOrElse("")}," +
      s"rtt=${rtt.map(x => s"${x}i").getOrElse("")}," +
      s"total_additional=${total_additional.map(x => s"${x}i").getOrElse("")}," +
      s"total_answer=${total_answer.map(x => s"${x}i").getOrElse("")}," +
      s"total_authority=${total_authority.map(x => s"${x}i").getOrElse("")}," +
      s"ttl=${ttl.map(x => s"${x}i").getOrElse("")} " +
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
