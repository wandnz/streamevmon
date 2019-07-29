package nz.net.wand.amp.analyser

import nz.net.wand.amp.analyser.measurements.{DNSMeta, ICMPMeta, TracerouteMeta}

object SeedData extends Configuration {
  val postgresConfigPrefix = "postgres.dataSource"
  val postgresData = "nntsc.sql"
  val postgresUsername: String = getConfigString(s"$postgresConfigPrefix.user").getOrElse("cuz")
  val postgresPassword: String = getConfigString(s"$postgresConfigPrefix.password").getOrElse("")

  val postgresDatabase: String =
    getConfigString(s"$postgresConfigPrefix.databaseName").getOrElse("nntsc")

  val expectedICMPMeta: Seq[ICMPMeta] = Seq(
    ICMPMeta(3, "amplet", "wand.net.nz", "ipv4", "random"),
    ICMPMeta(4, "amplet", "google.com", "ipv4", "random")
  )

  val expectedDNSMeta: Seq[DNSMeta] = Seq(
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

  val expectedTracerouteMeta: Seq[TracerouteMeta] = Seq(
    TracerouteMeta(5, "amplet", "google.com", "ipv4", "60"),
    TracerouteMeta(6, "amplet", "wand.net.nz", "ipv4", "60")
  )
}
