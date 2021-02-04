/* This file is part of streamevmon.
 *
 * Copyright (C) 2021  The University of Waikato, Hamilton, New Zealand
 *
 * Author: Daniel Oosterwijk
 *
 * All rights reserved.
 *
 * This code has been developed by the University of Waikato WAND
 * research group. For further information please see https://wand.nz,
 * or our Github organisation at https://github.com/wanduow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.measurements.traits._

import java.time.{Instant, ZoneId}

/** Represents an AMP DNS measurement, as well as the metadata associated with
  * the scheduled test that generated it.
  *
  * @see [[DNS]]
  * @see [[DNSMeta]]
  * @see [[https://github.com/wanduow/amplet2/wiki/amp-dns]]
  */
case class RichDNS(
  stream          : String,
  source          : String,
  destination     : String,
  instance        : String,
  address         : String,
  query           : String,
  query_type      : String,
  query_class     : String,
  udp_payload_size: Int,
  recurse         : Boolean,
  dnssec          : Boolean,
  nsid            : Boolean,
  flag_aa         : Option[Boolean],
  flag_ad         : Option[Boolean],
  flag_cd         : Option[Boolean],
  flag_qr         : Option[Boolean],
  flag_ra         : Option[Boolean],
  flag_rd         : Option[Boolean],
  flag_tc         : Option[Boolean],
  lossrate        : Option[Double],
  opcode          : Option[Int],
  query_len       : Option[Int],
  rcode           : Option[Int],
  requests        : Int,
  response_size   : Option[Int],
  rtt             : Option[Int],
  total_additional: Option[Int],
  total_answer    : Option[Int],
  total_authority : Option[Int],
  ttl             : Option[Int],
  time            : Instant
) extends RichInfluxMeasurement {

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
      s"opcode=${opcode.getOrElse("")}," +
      s"query_len=${query_len.getOrElse("")}," +
      s"rcode=${rcode.getOrElse("")}," +
      s"requests=$requests," +
      s"response_size=${response_size.getOrElse("")}," +
      s"rtt=${rtt.getOrElse("")}," +
      s"total_additional=${total_additional.getOrElse("")}," +
      s"total_answer=${total_answer.getOrElse("")}," +
      s"total_authority=${total_authority.getOrElse("")}," +
      s"ttl=${ttl.getOrElse("")} " +
      s"${time.atZone(ZoneId.systemDefault())}"
  }

  override def isLossy: Boolean = lossrate.getOrElse(1.0) > 0.0

  // Case classes don't generate unapply methods for more than 22 fields, since
  // the biggest Tuple is 22 items.
  override def toCsvFormat: Seq[String] = Seq(
    stream: String,
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
    lossrate: Option[Double],
    opcode: Option[Int],
    query_len: Option[Int],
    rcode: Option[Int],
    requests: Int,
    response_size: Option[Int],
    rtt: Option[Int],
    total_additional: Option[Int],
    total_answer: Option[Int],
    total_authority: Option[Int],
    ttl: Option[Int],
    time: Instant
  ).map(toCsvEntry)

  var defaultValue: Option[Double] = rtt.map(_.toDouble)
}

object RichDNS extends RichInfluxMeasurementFactory {

  override def create(base: InfluxMeasurement, meta: PostgresMeasurementMeta): Option[RichInfluxMeasurement] = {
    base match {
      case b: DNS =>
        meta match {
          case m: DNSMeta =>
            Some(
              RichDNS(
                m.stream.toString,
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
