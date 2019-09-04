package nz.net.wand.streamevmon.connectors

import nz.net.wand.streamevmon.SeedData

import java.sql.DriverManager

class PostgresConnectionTest extends PostgresContainerSpec {
  "PostgreSQL container" should {
    "successfully connect with raw JDBC" in {
      val connection =
        DriverManager.getConnection(container.jdbcUrl, container.username, container.password)
      assert(connection.isValid(0))
      connection.close()
    }

    "contain expected metadata" in {
      import PostgresSchema._
      import SquerylEntrypoint._

      assert(transaction(icmpMeta.allRows.toList) === SeedData.icmp.allExpectedMeta)
      assert(transaction(dnsMeta.allRows.toList) === SeedData.dns.allExpectedMeta)
      assert(transaction(tracerouteMeta.allRows.toList) === SeedData.traceroute.allExpectedMeta)
    }
  }
}
