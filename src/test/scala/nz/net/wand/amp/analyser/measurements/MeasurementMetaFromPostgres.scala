package nz.net.wand.amp.analyser.measurements

import nz.net.wand.amp.analyser._
import nz.net.wand.amp.analyser.connectors.{PostgresConnection, PreparePostgresTestContainer}

import java.sql.DriverManager

import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import org.scalatest.WordSpec

class MeasurementMetaFromPostgres extends WordSpec with ForAllTestContainer {

  override val container: PostgreSQLContainer = PreparePostgresTestContainer.get()

  override def afterStart(): Unit = {
    PreparePostgresTestContainer.run(container)
  }

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

      assertResult(SeedData.allExpectedICMPMeta)(transaction(icmpMeta.allRows.toList))
      assertResult(SeedData.allExpectedDNSMeta)(transaction(dnsMeta.allRows.toList))
      assertResult(SeedData.allExpectedTracerouteMeta)(transaction(tracerouteMeta.allRows.toList))
    }
  }

  "PostgresConnection" should {
    "obtain correct ICMPMeta" in {
      val result = PostgresConnection.getICMPMeta(SeedData.expectedICMP).get
      assertResult(SeedData.expectedICMPMeta)(result)
    }

    "obtain correct DNSMeta" in {
      val result = PostgresConnection.getDNSMeta(SeedData.expectedDNS).get
      assertResult(SeedData.expectedDNSMeta)(result)
    }

    "obtain correct TracerouteMeta" in {
      val result = PostgresConnection.getTracerouteMeta(SeedData.expectedTraceroute).get
      assertResult(SeedData.expectedTracerouteMeta)(result)
    }

    "obtain several correct Meta objects" in {
      Seq(
        PostgresConnection.getMeta(SeedData.expectedICMP),
        PostgresConnection.getMeta(SeedData.expectedDNS),
        PostgresConnection.getMeta(SeedData.expectedTraceroute)
      ).foreach {
        case Some(x) =>
          x match {
            case _: ICMPMeta       => assertResult(SeedData.expectedICMPMeta)(x)
            case _: DNSMeta        => assertResult(SeedData.expectedDNSMeta)(x)
            case _: TracerouteMeta => assertResult(SeedData.expectedTracerouteMeta)(x)
            case _                 => fail()
          }
        case None => fail()
      }
    }
  }

  "Children of Measurement.enrich" should {
    "obtain the correct RichICMP object" in {
      assertResult(SeedData.expectedRichICMP)(SeedData.expectedICMP.enrich().get)
    }

    "obtain the correct RichDNS object" in {
      assertResult(SeedData.expectedRichDNS)(SeedData.expectedDNS.enrich().get)
    }

    "obtain the correct RichTraceroute object" in {
      assertResult(SeedData.expectedRichTraceroute)(SeedData.expectedTraceroute.enrich().get)
    }
  }
}
