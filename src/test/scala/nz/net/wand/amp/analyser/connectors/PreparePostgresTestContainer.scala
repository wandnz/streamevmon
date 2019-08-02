package nz.net.wand.amp.analyser.connectors

import nz.net.wand.amp.analyser.SeedData

import java.sql.DriverManager

import com.dimafeng.testcontainers.PostgreSQLContainer
import org.squeryl.{Session, SessionFactory}
import org.squeryl.adapters.PostgreSqlAdapter

object PreparePostgresTestContainer {

  def get(): PostgreSQLContainer =
    PostgreSQLContainer("postgres:10")
      .configure(db => {
        db.withUsername(SeedData.postgresUsername)
        db.withPassword(SeedData.postgresPassword)
        db.withDatabaseName(SeedData.postgresDatabase)

        db.withInitScript(SeedData.postgresData)

        db.start()
        db.execInContainer(
          "bash",
          "-c",
          "sed " + "-i " +
            "s/host all all all md5/host all all all trust/g " +
            "/var/lib/postgresql/data/pg_hba.conf " +
            "&& " +
            "su postgres -c '/usr/lib/postgresql/10/bin/pg_ctl reload' " +
            "&& " +
            "echo 'Reloaded config' > /test.txt"
        )
        db.stop()
      })

  def run(db: PostgreSQLContainer): Unit = {
    setupTestConnection(db)
  }

  private[this] def setupTestConnection(db: PostgreSQLContainer): Unit = {
    SessionFactory.concreteFactory = Some(
      () =>
        Session.create(
          DriverManager.getConnection(db.jdbcUrl, db.username, db.password),
          new PostgreSqlAdapter
      ))
  }
}
