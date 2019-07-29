package nz.net.wand.amp.analyser

import nz.net.wand.amp.analyser.measurements.{PostgresSchema, Traceroute}

import java.sql.DriverManager

import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import org.scalatest.FlatSpec

class TestContainersPostgres extends FlatSpec with ForAllTestContainer {

  override val container: PostgreSQLContainer = PreparePostgresTestContainer.get()

  override def afterStart(): Unit = {
    PreparePostgresTestContainer.run(container)
  }

  "PostgreSQL container" should "successfully connect with raw JDBC" in {
    val connection =
      DriverManager.getConnection(container.jdbcUrl, container.username, container.password)
    assert(connection.isValid(0))
    connection.close()
  }

  "Squeryl" should "obtain expected metadata" in {
    println(container.jdbcUrl)
    import PostgresSchema._
    import SquerylEntrypoint._

    assert(SeedData.expectedICMPMeta === transaction(icmpMeta.allRows.toList))
    assert(SeedData.expectedDNSMeta === transaction(dnsMeta.allRows.toList))
    assert(SeedData.expectedTracerouteMeta === transaction(tracerouteMeta.allRows.toList))
  }

  "PostgresConnection" should "obtain correct ICMPMeta" in {
    println(PostgresConnection.jdbcUrl)

    println(
      PostgresConnection.getTracerouteMeta(
        Traceroute(
          "5",
          30.0,
          12345
        )))
  }
}
