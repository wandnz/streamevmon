package nz.net.wand.amp.analyser.measurements

import nz.net.wand.amp.analyser._

import java.sql.DriverManager

import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import org.scalatest.FlatSpec

class MeasurementMetaFromPostgres extends FlatSpec with ForAllTestContainer {

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
    import PostgresSchema._
    import SquerylEntrypoint._

    assert(SeedData.allExpectedICMPMeta === transaction(icmpMeta.allRows.toList))
    assert(SeedData.allExpectedDNSMeta === transaction(dnsMeta.allRows.toList))
    assert(SeedData.allExpectedTracerouteMeta === transaction(tracerouteMeta.allRows.toList))
  }

  "PostgresConnection" should "obtain correct ICMPMeta" in {
    val result = PostgresConnection.getICMPMeta(SeedData.expectedICMP).get
    assertResult(SeedData.expectedICMPMeta)(result)
  }

  "PostgresConnection" should "obtain correct DNSMeta" in {
    val result = PostgresConnection.getDNSMeta(SeedData.expectedDNS).get
    assertResult(SeedData.expectedDNSMeta)(result)
  }

  "PostgresConnection" should "obtain correct TracerouteMeta" in {
    val result = PostgresConnection.getTracerouteMeta(SeedData.expectedTraceroute).get
    assertResult(SeedData.expectedTracerouteMeta)(result)
  }

  "PostgresConnection.getMeta" should "obtain several correct Meta objects" in {
    Seq(
      PostgresConnection.getMeta(SeedData.expectedICMP),
      PostgresConnection.getMeta(SeedData.expectedDNS),
      PostgresConnection.getMeta(SeedData.expectedTraceroute)
    ).foreach {
      case Some(x) =>
        x match {
          case _: ICMPMeta       => assert(x === SeedData.expectedICMPMeta)
          case _: DNSMeta        => assert(x === SeedData.expectedDNSMeta)
          case _: TracerouteMeta => assert(x === SeedData.expectedTracerouteMeta)
          case _                 => fail()
        }
      case None => fail()
    }
  }

  "ICMP.enrich" should "obtain the correct RichICMP object" in {
    assertResult(SeedData.expectedRichICMP)(SeedData.expectedICMP.enrich().get)
  }

  "DNS.enrich" should "obtain the correct RichDNS object" in {
    assertResult(SeedData.expectedRichDNS)(SeedData.expectedDNS.enrich().get)
  }

  "Traceroute.enrich" should "obtain the correct RichTraceroute object" in {
    assertResult(SeedData.expectedRichTraceroute)(SeedData.expectedTraceroute.enrich().get)
  }
}
