package nz.net.wand.amp.analyser.connectors

import nz.net.wand.amp.analyser.{Caching, Configuration}
import nz.net.wand.amp.analyser.measurements._

import java.sql.DriverManager

import org.squeryl.{Session, SessionFactory}
import org.squeryl.adapters.PostgreSqlAdapter

/** PostgreSQL interface which produces
  * [[nz.net.wand.amp.analyser.measurements.MeasurementMeta MeasurementMeta]]
  * objects.
  *
  * @see [[InfluxConnection]]
  */
object PostgresConnection extends Caching with Configuration {

  configPrefix = "connectors.postgres.dataSource"

  /** Connection string for PostgreSQL. */
  private[this] val jdbcUrl: String = {
    val host = getConfigString("serverName").getOrElse("localhost")
    val port = getConfigString("portNumber").getOrElse("5432")
    val databaseName = getConfigString("databaseName").getOrElse("nntsc")
    s"jdbc:postgresql://$host:$port/$databaseName?loggerLevel=OFF"
  }

  /** The username that should be used to connect to PostgreSQL */
  private[this] val username: String = getConfigString("user").getOrElse("cuz")

  /** The password that should be used to connect to PostgreSQL */
  private[this] val password: String = getConfigString("password").getOrElse("")

  /** Ensures that there is an existing connection to PostgreSQL.
    */
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

  /** Gets the [[nz.net.wand.amp.analyser.measurements.ICMPMeta metadata]]
    * associated with a given [[nz.net.wand.amp.analyser.measurements.ICMP ICMP]]
    * measurement.
    *
    * @param base The measurement to gather metadata for.
    *
    * @return The [[nz.net.wand.amp.analyser.measurements.ICMPMeta metadata]] if
    *         successful, otherwise None.
    */
  private[connectors] def getICMPMeta(base: ICMP): Option[ICMPMeta] = {
    getWithCache(
      s"icmp.${base.stream}", {
        getOrInitSession()
        import nz.net.wand.amp.analyser.connectors.PostgresSchema._
        import nz.net.wand.amp.analyser.connectors.SquerylEntrypoint._

        transaction(icmpMeta.where(m => m.stream === base.stream).headOption)
      }
    )
  }

  /** Gets the [[nz.net.wand.amp.analyser.measurements.DNSMeta metadata]]
    * associated with a given [[nz.net.wand.amp.analyser.measurements.DNS DNS]]
    * measurement.
    *
    * @param base The measurement to gather metadata for.
    *
    * @return The [[nz.net.wand.amp.analyser.measurements.DNSMeta metadata]] if
    *         successful, otherwise None.
    */
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

  /** Gets the [[nz.net.wand.amp.analyser.measurements.TracerouteMeta metadata]]
    * associated with a given [[nz.net.wand.amp.analyser.measurements.Traceroute Traceroute]]
    * measurement.
    *
    * @param base The measurement to gather metadata for.
    *
    * @return The [[nz.net.wand.amp.analyser.measurements.TracerouteMeta metadata]]
    *         if successful, otherwise None.
    */
  private[connectors] def getTracerouteMeta(base: Traceroute): Option[TracerouteMeta] = {
    getWithCache(
      s"traceroute.${base.stream}", {
        getOrInitSession()
        import nz.net.wand.amp.analyser.connectors.PostgresSchema._
        import nz.net.wand.amp.analyser.connectors.SquerylEntrypoint._

        transaction(tracerouteMeta.where(m => m.stream === base.stream).headOption)
      }
    )
  }

  /** Gets the [[nz.net.wand.amp.analyser.measurements.TCPPingMeta metadata]]
    * associated with a given [[nz.net.wand.amp.analyser.measurements.TCPPing TCPPing]]
    * measurement.
    *
    * @param base The measurement to gather metadata for.
    *
    * @return The [[nz.net.wand.amp.analyser.measurements.TCPPingMeta metadata]]
    *         if successful, otherwise None.
    */
  private[connectors] def getTcppingMeta(base: TCPPing): Option[TCPPingMeta] = {
    getWithCache(
      s"tcpping.${base.stream}", {
        getOrInitSession()
        import nz.net.wand.amp.analyser.connectors.PostgresSchema._
        import nz.net.wand.amp.analyser.connectors.SquerylEntrypoint._

        transaction(tcppingMeta.where(m => m.stream === base.stream).headOption)
      }
    )
  }

  /** Gets the [[nz.net.wand.amp.analyser.measurements.HTTPMeta metadata]]
    * associated with a given [[nz.net.wand.amp.analyser.measurements.HTTP HTTP]]
    * measurement.
    *
    * @param base The measurement to gather metadata for.
    *
    * @return The [[nz.net.wand.amp.analyser.measurements.HTTPMeta metadata]] if
    *         successful, otherwise None.
    */
  private[connectors] def getHttpMeta(base: HTTP): Option[HTTPMeta] = {
    getWithCache(
      s"http.${base.stream}", {
        getOrInitSession()
        import nz.net.wand.amp.analyser.connectors.PostgresSchema._
        import nz.net.wand.amp.analyser.connectors.SquerylEntrypoint._

        transaction(httpMeta.where(m => m.stream === base.stream).headOption)
      }
    )
  }

  /** Gets the metadata associated with a given measurement.
    *
    * @param base The measurement to gather metadata for.
    *
    * @return The metadata if successful, otherwise None.
    */
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
