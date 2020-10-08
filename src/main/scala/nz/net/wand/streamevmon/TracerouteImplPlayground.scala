package nz.net.wand.streamevmon

import nz.net.wand.streamevmon.measurements.amp.{Traceroute, TracerouteAsPath, TraceroutePath}

import java.sql.DriverManager

import org.postgresql.util.PSQLException
import org.squeryl.{Session, SessionFactory}
import org.squeryl.adapters.PostgreSqlAdapter

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

  def main(args: Array[String]): Unit = {
    assert(getOrInitSession())
    import nz.net.wand.streamevmon.connectors.postgres.PostgresSchema._
    import nz.net.wand.streamevmon.connectors.postgres.SquerylEntrypoint._

    val result = transaction(traceroute(9).allRows.head)

    println(result)

    val aspath = getTracerouteAsPath(result)

    println(aspath)

    val breakpoint = 1
  }
}
