package nz.net.wand.streamevmon.connectors

import nz.net.wand.streamevmon.{PostgresContainerSpec, SeedData}

import java.sql.DriverManager

class PostgresConnectionTest extends PostgresContainerSpec {
  "PostgreSQL container" should {
    "successfully connect with raw JDBC" in {
      val connection =
        DriverManager.getConnection(container.jdbcUrl, container.username, container.password)
      connection.isValid(0) shouldBe true
      connection.close()
    }

    "contain expected metadata" in {
      import nz.net.wand.streamevmon.connectors.postgres.PostgresSchema._
      import nz.net.wand.streamevmon.connectors.postgres.SquerylEntrypoint._

      transaction(icmpMeta.allRows.toList) shouldBe SeedData.icmp.allExpectedMeta
      transaction(dnsMeta.allRows.toList) shouldBe SeedData.dns.allExpectedMeta
      transaction(tracerouteMeta.allRows.toList) shouldBe SeedData.traceroute.allExpectedMeta
    }
  }
}
