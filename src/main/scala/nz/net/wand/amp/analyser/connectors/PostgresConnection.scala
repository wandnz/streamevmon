package nz.net.wand.amp.analyser.connectors

import nz.net.wand.amp.analyser.{Caching, Configuration, Logging}
import nz.net.wand.amp.analyser.measurements._

import java.sql.DriverManager

import org.squeryl.{Session, SessionFactory}
import org.squeryl.adapters.PostgreSqlAdapter

object PostgresConnection extends Logging with Configuration with Caching {

  configPrefix = "connectors.postgres.dataSource"

  val jdbcUrl: String = {
    val host = getConfigString("serverName").getOrElse("localhost")
    val port = getConfigString("portNumber").getOrElse("5432")
    val databaseName = getConfigString("databaseName").getOrElse("nntsc")
    s"jdbc:postgresql://$host:$port/$databaseName?loggerLevel=OFF"
  }
  val username: String = getConfigString("user").getOrElse("cuz")
  val password: String = getConfigString("password").getOrElse("")

  private[this] def getOrInitSession(): Unit =
    SessionFactory.concreteFactory match {
      case Some(_) =>
      case None =>
        SessionFactory.concreteFactory = Some(
          () =>
            Session.create(
              DriverManager.getConnection(jdbcUrl, username, password),
              new PostgreSqlAdapter
          ))
    }

  def getICMPMeta(base: ICMP): Option[ICMPMeta] = {
    getWithCache(
      s"icmp.${base.stream}", {
        getOrInitSession()
        import nz.net.wand.amp.analyser.connectors.PostgresSchema._
        import nz.net.wand.amp.analyser.connectors.SquerylEntrypoint._

        transaction(icmpMeta.where(m => m.stream === base.stream).headOption)
      }
    )
  }

  def getDNSMeta(base: DNS): Option[DNSMeta] = {
    getWithCache(
      s"dns.${base.stream}", {
        getOrInitSession()
        import nz.net.wand.amp.analyser.connectors.PostgresSchema._
        import nz.net.wand.amp.analyser.connectors.SquerylEntrypoint._

        transaction(dnsMeta.where(m => m.stream === base.stream).headOption)
      }
    )
  }

  def getTracerouteMeta(base: Traceroute): Option[TracerouteMeta] = {
    getWithCache(
      s"traceroute.${base.stream}", {
        getOrInitSession()
        import nz.net.wand.amp.analyser.connectors.PostgresSchema._
        import nz.net.wand.amp.analyser.connectors.SquerylEntrypoint._

        transaction(tracerouteMeta.where(m => m.stream === base.stream).headOption)
      }
    )
  }

  def getTcppingMeta(base: TCPPing): Option[TCPPingMeta] = {
    getWithCache(
      s"tcpping.${base.stream}", {
        getOrInitSession()
        import nz.net.wand.amp.analyser.connectors.PostgresSchema._
        import nz.net.wand.amp.analyser.connectors.SquerylEntrypoint._

        transaction(tcppingMeta.where(m => m.stream === base.stream).headOption)
      }
    )
  }

  def getHttpMeta(base: HTTP): Option[HTTPMeta] = {
    getWithCache(
      s"http.${base.stream}", {
        getOrInitSession()
        import nz.net.wand.amp.analyser.connectors.PostgresSchema._
        import nz.net.wand.amp.analyser.connectors.SquerylEntrypoint._

        transaction(httpMeta.where(m => m.stream === base.stream).headOption)
      }
    )
  }

  def getMeta(base: Measurement): Option[MeasurementMeta] =
    base match {
      case x: ICMP       => getICMPMeta(x)
      case x: DNS        => getDNSMeta(x)
      case x: Traceroute => getTracerouteMeta(x)
      case x: TCPPing    => getTcppingMeta(x)
      case x: HTTP       => getHttpMeta(x)
      case _             => None
    }
}
