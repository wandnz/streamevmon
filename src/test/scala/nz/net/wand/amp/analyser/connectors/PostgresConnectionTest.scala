package nz.net.wand.amp.analyser.connectors

import nz.net.wand.amp.analyser.SeedData

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
      import nz.net.wand.amp.analyser.connectors.PostgresSchema._
      import nz.net.wand.amp.analyser.connectors.SquerylEntrypoint._

      assertResult(SeedData.icmp.allExpectedMeta)(transaction(icmpMeta.allRows.toList))
      assertResult(SeedData.dns.allExpectedMeta)(transaction(dnsMeta.allRows.toList))
      assertResult(SeedData.traceroute.allExpectedMeta)(transaction(tracerouteMeta.allRows.toList))
    }
  }
}
