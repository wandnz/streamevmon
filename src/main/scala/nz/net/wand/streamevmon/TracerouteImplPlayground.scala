package nz.net.wand.streamevmon

import nz.net.wand.streamevmon.measurements.amp._

import java.sql.DriverManager
import java.time.Instant

import org.postgresql.util.PSQLException
import org.squeryl.{Session, SessionFactory}
import org.squeryl.adapters.PostgreSqlAdapter

import scala.util.Try

object TracerouteImplPlayground extends Logging {
  val jdbcUrl = s"jdbc:postgresql://localhost:5432/nntsc?loggerLevel=OFF"
  val user = "cuz"
  val password = ""

  protected def getOrInitSession(): Boolean = {
    SessionFactory.concreteFactory match {
      case Some(_) => true
      case None =>
        SessionFactory.concreteFactory = {
          try {
            DriverManager.getConnection(jdbcUrl, user, password)

            Some(() => {
              Class.forName("org.postgresql.Driver")
              val s = Session.create(
                DriverManager.getConnection(jdbcUrl, user, password),
                new PostgreSqlAdapter
              )
              s.bindToCurrentThread
              s
            })
          }
          catch {
            case e: PSQLException =>
              logger.error(s"Could not initialise PostgreSQL session! $e")
              None
          }
        }
        SessionFactory.concreteFactory.isDefined
    }
  }

  def checkTracerouteStreamIsInt(trace: Traceroute): Unit = {
    try {
      trace.stream.toInt
    }
    catch {
      case e: Throwable => throw new IllegalArgumentException(
        s"Traceroute had invalid stream ID ${trace.stream}!",
        e
      )
    }
  }

  def getTraceroutePath(stream: Int, pathId: Int): TraceroutePath = {
    assert(getOrInitSession())
    import nz.net.wand.streamevmon.connectors.postgres.PostgresSchema._
    import nz.net.wand.streamevmon.connectors.postgres.SquerylEntrypoint._

    transaction(traceroutePath(stream).where(_.path_id === pathId).head)
  }

  def getTraceroutePath(trace: Traceroute): TraceroutePath = {
    checkTracerouteStreamIsInt(trace)
    getTraceroutePath(trace.stream.toInt, trace.path_id)
  }

  def getTracerouteAsPath(stream: Int, asPathId: Int): TracerouteAsPath = {
    assert(getOrInitSession())
    import nz.net.wand.streamevmon.connectors.postgres.PostgresSchema._
    import nz.net.wand.streamevmon.connectors.postgres.SquerylEntrypoint._

    transaction(tracerouteAsPath(stream).where(_.aspath_id === asPathId).head)
  }

  def getTracerouteAsPath(trace: Traceroute): Option[TracerouteAsPath] = {
    checkTracerouteStreamIsInt(trace)
    trace.aspath_id.map(asPathId => getTracerouteAsPath(trace.stream.toInt, asPathId))
  }


  def getTracerouteData(
    stream: Int,
    start: Instant = Instant.EPOCH,
    end   : Instant = Instant.now()
  ): Option[Iterable[Traceroute]] = {
    if (!getOrInitSession()) {
      None
    }
    else {
      import nz.net.wand.streamevmon.connectors.postgres.PostgresSchema._
      import nz.net.wand.streamevmon.connectors.postgres.SquerylEntrypoint._

      Try(
        transaction(
          from(traceroute(stream))(t =>
            where(
              t.timestamp between(start.getEpochSecond, end.getEpochSecond)
            ).select(t)
          ).toList
        )
      ).fold(
        e => {
          logger.error(s"Failed Postgres transaction! $e")
          throw e
          None
        },
        tr => Some(tr)
      )
    }
  }


  def getAllTracerouteMeta: Option[Iterable[TracerouteMeta]] = {
    if (!getOrInitSession()) {
      None
    }
    else {
      import nz.net.wand.streamevmon.connectors.postgres.PostgresSchema._
      import nz.net.wand.streamevmon.connectors.postgres.SquerylEntrypoint._

      Try(
        transaction(from(tracerouteMeta)(m => select(m)).toList)
      ).fold(
        e => {
          logger.error(s"Failed Postgres transaction! $e")
          None
        },
        tr => Some(tr)
      )
    }
  }

  def main(args: Array[String]): Unit = {
    assert(getOrInitSession())

    getAllTracerouteMeta.foreach(_.foreach(println))

    val breakpoint = 1
  }
}
