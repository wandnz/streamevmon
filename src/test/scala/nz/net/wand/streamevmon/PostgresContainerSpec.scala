package nz.net.wand.streamevmon

import nz.net.wand.streamevmon.connectors.PostgresConnection

import java.sql.DriverManager

import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import org.squeryl.{Session, SessionFactory}
import org.squeryl.adapters.PostgreSqlAdapter

trait PostgresContainerSpec extends TestBase with ForAllTestContainer {

  // We know that NNTSC uses postgres 10, so we might as well stick with
  // that version. Alpine for size benefits.
  override val container: PostgreSQLContainer = PostgreSQLContainer("postgres:10-alpine")
    .configure(db => {
      val params = Configuration.get()

      db.withUsername(params.get("source.postgres.user"))
      db.withPassword(params.get("source.postgres.password"))
      db.withDatabaseName(params.get("source.postgres.databaseName"))

      // Postgres requires either this or an admin password to be set.
      // We choose trust authentication because we simply don't care about
      // security for ephemeral containers which can just be re-run in the
      // unlikely situation that we get attacked.
      db.addEnv("POSTGRES_HOST_AUTH_METHOD", "trust")

      db.withInitScript("nntsc.sql")
    })

  override def afterStart(): Unit = {
    SessionFactory.concreteFactory = Some(
      () =>
        Session.create(
          DriverManager.getConnection(container.jdbcUrl, container.username, container.password),
          new PostgreSqlAdapter
      ))
  }

  protected def getPostgres: PostgresConnection = {
    PostgresConnection(
      container.jdbcUrl,
      container.username,
      container.password,
      caching_ttl = 0
    )
  }
}
