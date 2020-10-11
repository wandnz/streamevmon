package nz.net.wand.streamevmon.connectors

import nz.net.wand.streamevmon.{PostgresContainerSpec, SeedData}
import nz.net.wand.streamevmon.connectors.postgres.PostgresConnection

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

  "PostgresConnection" should {
    def getConn = PostgresConnection(container.jdbcUrl, container.username, container.password, 0)

    "retrieve all TracerouteMeta entries" in {
      getConn.getAllTracerouteMeta shouldBe Some(SeedData.traceroute.allExpectedMeta)
    }

    "retrieve an expected Traceroute entry" in {
      getConn.getTracerouteData(18).get should contain(SeedData.traceroute.expected)
    }

    "retrieve the expected TraceroutePath entry" in {
      getConn.getTraceroutePath(getConn.getTracerouteData(18).get.head) shouldBe Some(SeedData.traceroute.expectedPath)
    }

    "retrieve the expected TracerouteAsPath entry" in {
      getConn.getTracerouteAsPath(getConn.getTracerouteData(18).get.head) shouldBe Some(SeedData.traceroute.expectedAsPath)
    }
  }
}
