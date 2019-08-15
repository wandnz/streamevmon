package nz.net.wand.amp.analyser

import nz.net.wand.amp.analyser.events._
import nz.net.wand.amp.analyser.measurements._

import java.time.Instant
import java.util.concurrent.TimeUnit

import org.apache.flink.streaming.connectors.influxdb.InfluxDBPoint

import scala.collection.JavaConversions.mapAsJavaMap

object SeedData extends Configuration {

  object postgres {

    val configPrefix = "postgres.dataSource"
    val dataFile = "nntsc.sql"
    val username: String = getConfigString(s"$configPrefix.user").getOrElse("cuz")
    val password: String = getConfigString(s"$configPrefix.password").getOrElse("")

    val database: String =
      getConfigString(s"$configPrefix.databaseName").getOrElse("nntsc")
  }

  object icmp {

    val allExpectedMeta: Seq[ICMPMeta] = Seq(
      ICMPMeta(3, "amplet", "wand.net.nz", "ipv4", "random"),
      ICMPMeta(4, "amplet", "google.com", "ipv4", "random"),
      ICMPMeta(10, "amplet", "wand.net.nz", "ipv4", "84"),
      ICMPMeta(11, "amplet", "cloud.google.com", "ipv4", "84"),
      ICMPMeta(12, "amplet", "www.cloudflare.com", "ipv4", "84"),
      ICMPMeta(13, "amplet", "afrinic.net", "ipv4", "84"),
      ICMPMeta(14, "amplet", "download.microsoft.com", "ipv4", "84")
    )

    val subscriptionLine =
      "data_amp_icmp,stream=3 loss=0i,lossrate=0.0,median=225i,packet_size=520i,results=1i,rtts=\"[225]\" 1563761840000000000"

    val expected = ICMP(
      stream = 3,
      loss = 0,
      lossrate = 0.0,
      median = Some(225),
      packet_size = 520,
      results = 1,
      rtts = Seq(Some(225)),
      time = Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(1563761840000000000L))
    )

    val expectedMeta = ICMPMeta(
      stream = 3,
      source = "amplet",
      destination = "wand.net.nz",
      family = "ipv4",
      packet_size_selection = "random"
    )

    val expectedRich = RichICMP(
      stream = 3,
      source = "amplet",
      destination = "wand.net.nz",
      family = "ipv4",
      packet_size_selection = "random",
      loss = 0,
      lossrate = 0.0,
      median = Some(225),
      packet_size = 520,
      results = 1,
      rtts = Seq(Some(225)),
      time = Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(1563761840000000000L))
    )
  }

  object dns {

    val allExpectedMeta: Seq[DNSMeta] = Seq(
      DNSMeta(
        1,
        source = "amplet",
        destination = "8.8.8.8",
        instance = "8.8.8.8",
        address = "8.8.8.8",
        query = "wand.net.nz",
        query_type = "AAAA",
        query_class = "IN",
        udp_payload_size = 4096,
        recurse = false,
        dnssec = false,
        nsid = false
      ),
      DNSMeta(
        2,
        source = "amplet",
        destination = "1.1.1.1",
        instance = "1.1.1.1",
        address = "1.1.1.1",
        query = "wand.net.nz",
        query_type = "AAAA",
        query_class = "IN",
        udp_payload_size = 4096,
        recurse = false,
        dnssec = false,
        nsid = false
      ),
      DNSMeta(
        15,
        source = "amplet",
        destination = "ns1.dns.net.nz",
        instance = "ns1.dns.net.nz",
        address = "202.46.190.130",
        query = "dns.net.nz",
        query_type = "NS",
        query_class = "IN",
        udp_payload_size = 4096,
        recurse = false,
        dnssec = false,
        nsid = false
      ),
      DNSMeta(
        16,
        source = "amplet",
        destination = "a.root-servers.net",
        instance = "a.root-servers.net",
        address = "198.41.0.4",
        query = "example.com",
        query_type = "NS",
        query_class = "IN",
        udp_payload_size = 4096,
        recurse = false,
        dnssec = false,
        nsid = false
      )
    )

    val subscriptionLine =
      "data_amp_dns,stream=1 flag_aa=False,flag_ad=False,flag_cd=False,flag_qr=True,flag_ra=True,flag_rd=False,flag_tc=False,lossrate=0.0,opcode=0i,query_len=40i,rcode=0i,requests=1i,response_size=68i,rtt=35799i,total_additional=1i,total_answer=1i,total_authority=0i,ttl=0i 1563761841000000000"

    val expected = DNS(
      stream = 1,
      flag_aa = Some(false),
      flag_ad = Some(false),
      flag_cd = Some(false),
      flag_qr = Some(true),
      flag_ra = Some(true),
      flag_rd = Some(false),
      flag_tc = Some(false),
      lossrate = 0.0,
      opcode = Some(0),
      query_len = 40,
      rcode = Some(0),
      requests = 1,
      response_size = Some(68),
      rtt = Some(35799),
      total_additional = Some(1),
      total_answer = Some(1),
      total_authority = Some(0),
      ttl = Some(0),
      time = Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(1563761841000000000L))
    )

    val expectedMeta = DNSMeta(
      1,
      source = "amplet",
      destination = "8.8.8.8",
      instance = "8.8.8.8",
      address = "8.8.8.8",
      query = "wand.net.nz",
      query_type = "AAAA",
      query_class = "IN",
      udp_payload_size = 4096,
      recurse = false,
      dnssec = false,
      nsid = false
    )

    val expectedRich = RichDNS(
      stream = 1,
      source = "amplet",
      destination = "8.8.8.8",
      instance = "8.8.8.8",
      address = "8.8.8.8",
      query = "wand.net.nz",
      query_type = "AAAA",
      query_class = "IN",
      udp_payload_size = 4096,
      recurse = false,
      dnssec = false,
      nsid = false,
      flag_aa = Some(false),
      flag_ad = Some(false),
      flag_cd = Some(false),
      flag_qr = Some(true),
      flag_ra = Some(true),
      flag_rd = Some(false),
      flag_tc = Some(false),
      lossrate = 0.0,
      opcode = Some(0),
      query_len = 40,
      rcode = Some(0),
      requests = 1,
      response_size = Some(68),
      rtt = Some(35799),
      total_additional = Some(1),
      total_answer = Some(1),
      total_authority = Some(0),
      ttl = Some(0),
      time = Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(1563761841000000000L))
    )
  }

  object traceroute {

    val allExpectedMeta: Seq[TracerouteMeta] = Seq(
      TracerouteMeta(5, "amplet", "google.com", "ipv4", "60"),
      TracerouteMeta(6, "amplet", "wand.net.nz", "ipv4", "60"),
      TracerouteMeta(18, "amplet", "a.root-servers.net", "ipv4", "60"),
      TracerouteMeta(19, "amplet", "afrinic.net", "ipv4", "60")
    )

    val subscriptionLine =
      "data_amp_traceroute_pathlen,stream=5 path_length=12.0 1563761842000000000"

    val expected = Traceroute(
      stream = 5,
      path_length = 12.0,
      time = Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(1563761842000000000L))
    )

    val expectedMeta = TracerouteMeta(
      stream = 5,
      source = "amplet",
      destination = "google.com",
      family = "ipv4",
      packet_size_selection = "60"
    )

    val expectedRich = RichTraceroute(
      stream = 5,
      source = "amplet",
      destination = "google.com",
      family = "ipv4",
      packet_size_selection = "60",
      path_length = 12.0,
      time = Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(1563761842000000000L))
    )
  }

  object tcpping {

    val subscriptionLine =
      "data_amp_tcpping,stream=9 icmperrors=0i,loss=0i,lossrate=0.0,median=189i,packet_size=64i,results=1i,rtts=\"[189]\" 1564713040000000000"

    val expected = TCPPing(
      stream = 9,
      icmperrors = 0,
      loss = 0,
      lossrate = 0.0,
      median = Some(189),
      packet_size = 64,
      results = 1,
      rtts = Seq(Some(189)),
      time = Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(1564713040000000000L))
    )

    val expectedMeta = TCPPingMeta(
      stream = 9,
      source = "amplet",
      destination = "wand.net.nz",
      port = 443,
      family = "ipv4",
      packet_size_selection = "64"
    )

    val expectedRich = RichTCPPing(
      stream = 9,
      source = "amplet",
      destination = "wand.net.nz",
      port = 443,
      family = "ipv4",
      packet_size_selection = "64",
      icmperrors = 0,
      loss = 0,
      lossrate = 0.0,
      median = Some(189),
      packet_size = 64,
      results = 1,
      rtts = Seq(Some(189)),
      time = Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(1564713040000000000L))
    )
  }

  object http {

    val subscriptionLine =
      "data_amp_http,stream=17 bytes=62210i,duration=77i,object_count=8i,server_count=1i 1564713045000000000"

    val expected = HTTP(
      stream = 17,
      bytes = 62210,
      duration = 77,
      object_count = 8,
      server_count = 1,
      time = Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(1564713045000000000L))
    )

    val expectedMeta = HTTPMeta(
      stream = 17,
      source = "amplet",
      destination = "https://wand.net.nz/",
      max_connections = 24,
      max_connections_per_server = 8,
      max_persistent_connections_per_server = 2,
      pipelining_max_requests = 4,
      persist = true,
      pipelining = false,
      caching = false
    )

    val expectedRich = RichHTTP(
      stream = 17,
      source = "amplet",
      destination = "https://wand.net.nz/",
      max_connections = 24,
      max_connections_per_server = 8,
      max_persistent_connections_per_server = 2,
      pipelining_max_requests = 4,
      persist = true,
      pipelining = false,
      caching = false,
      bytes = 62210,
      duration = 77,
      object_count = 8,
      server_count = 1,
      time = Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(1564713045000000000L))
    )
  }

  object thresholdEvent {

    val event: ThresholdEvent = ThresholdEvent(
      severity = 10,
      time = Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(1564713045000000000L))
    )

    val influxPoint: InfluxDBPoint = new InfluxDBPoint(
      ThresholdEvent.measurement_name,
      event.time.toEpochMilli,
      mapAsJavaMap(Map()),
      mapAsJavaMap(
        Map[String, Object](
          "severity" -> new Integer(event.severity)
        ))
    )
  }
}
