package nz.net.wand.amp.analyser

import nz.net.wand.amp.analyser.measurements._

import java.time.Instant
import java.util.concurrent.TimeUnit

object SeedData extends Configuration {
  val postgresConfigPrefix = "postgres.dataSource"
  val postgresData = "nntsc.sql"
  val postgresUsername: String = getConfigString(s"$postgresConfigPrefix.user").getOrElse("cuz")
  val postgresPassword: String = getConfigString(s"$postgresConfigPrefix.password").getOrElse("")

  val postgresDatabase: String =
    getConfigString(s"$postgresConfigPrefix.databaseName").getOrElse("nntsc")

  val allExpectedICMPMeta: Seq[ICMPMeta] = Seq(
    ICMPMeta(3, "amplet", "wand.net.nz", "ipv4", "random"),
    ICMPMeta(4, "amplet", "google.com", "ipv4", "random")
  )
  val expectedICMPMeta = ICMPMeta(3, "amplet", "wand.net.nz", "ipv4", "random")

  val icmpSubscriptionLine =
    "data_amp_icmp,stream=3 loss=0i,lossrate=0.0,median=225i,packet_size=520i,results=1i,rtts=\"[225]\" 1563761840000000000"

  val expectedICMP = ICMP(3,
                          0,
                          0.0,
                          Some(225),
                          520,
                          1,
                          Seq(225),
                          Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(1563761840000000000L)))

  val expectedRichICMP = RichICMP(
    3,
    "amplet",
    "wand.net.nz",
    "ipv4",
    "random",
    0,
    0.0,
    Some(225),
    520,
    1,
    Seq(225),
    Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(1563761840000000000L))
  )

  val allExpectedDNSMeta: Seq[DNSMeta] = Seq(
    DNSMeta(1,
            "amplet",
            "8.8.8.8",
            "8.8.8.8",
            "8.8.8.8",
            "wand.net.nz",
            "AAAA",
            "IN",
            4096,
            recurse = false,
            dnssec = false,
            nsid = false),
    DNSMeta(2,
            "amplet",
            "1.1.1.1",
            "1.1.1.1",
            "1.1.1.1",
            "wand.net.nz",
            "AAAA",
            "IN",
            4096,
            recurse = false,
            dnssec = false,
            nsid = false)
  )

  val expectedDNSMeta = DNSMeta(1,
                                "amplet",
                                "8.8.8.8",
                                "8.8.8.8",
                                "8.8.8.8",
                                "wand.net.nz",
                                "AAAA",
                                "IN",
                                4096,
                                recurse = false,
                                dnssec = false,
                                nsid = false)

  val dnsSubscriptionLine =
    "data_amp_dns,stream=1 flag_aa=False,flag_ad=False,flag_cd=False,flag_qr=True,flag_ra=True,flag_rd=False,flag_tc=False,lossrate=0.0,opcode=0i,query_len=40i,rcode=0i,requests=1i,response_size=68i,rtt=35799i,total_additional=1i,total_answer=1i,total_authority=0i,ttl=0i 1563761810000000000"

  val expectedDNS = DNS(
    1,
    Some(false),
    Some(false),
    Some(false),
    Some(true),
    Some(true),
    Some(false),
    Some(false),
    0.0,
    Some(0),
    40,
    Some(0),
    1,
    Some(68),
    Some(35799),
    Some(1),
    Some(1),
    Some(0),
    Some(0),
    Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(1563761810000000000L))
  )

  val expectedRichDNS = RichDNS(
    1,
    "amplet",
    "8.8.8.8",
    "8.8.8.8",
    "8.8.8.8",
    "wand.net.nz",
    "AAAA",
    "IN",
    4096,
    recurse = false,
    dnssec = false,
    nsid = false,
    Some(false),
    Some(false),
    Some(false),
    Some(true),
    Some(true),
    Some(false),
    Some(false),
    0.0,
    Some(0),
    40,
    Some(0),
    1,
    Some(68),
    Some(35799),
    Some(1),
    Some(1),
    Some(0),
    Some(0),
    Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(1563761810000000000L))
  )

  val allExpectedTracerouteMeta: Seq[TracerouteMeta] = Seq(
    TracerouteMeta(5, "amplet", "google.com", "ipv4", "60"),
    TracerouteMeta(6, "amplet", "wand.net.nz", "ipv4", "60")
  )
  val expectedTracerouteMeta = TracerouteMeta(5, "amplet", "google.com", "ipv4", "60")

  val tracerouteSubscriptionLine =
    "data_amp_traceroute_pathlen,stream=5 path_length=12.0 1563764080000000000"

  val expectedTraceroute =
    Traceroute(5, 12.0, Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(1563764080000000000L)))

  val expectedRichTraceroute = RichTraceroute(
    5,
    "amplet",
    "google.com",
    "ipv4",
    "60",
    12.0,
    Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(1563764080000000000L))
  )
}
