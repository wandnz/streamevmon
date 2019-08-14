package nz.net.wand.amp.analyser.connectors

import nz.net.wand.amp.analyser.SeedData

import java.sql.DriverManager

import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import org.scalatest.WordSpec
import org.squeryl.{Session, SessionFactory}
import org.squeryl.adapters.PostgreSqlAdapter

class PostgresContainerSpec extends WordSpec with ForAllTestContainer {

  override val container: PostgreSQLContainer = PostgreSQLContainer("postgres:10")
    .configure(db => {
      db.withUsername(SeedData.postgres.username)
      db.withPassword(SeedData.postgres.password)
      db.withDatabaseName(SeedData.postgres.database)

      db.withInitScript(SeedData.postgres.dataFile)

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

  override def afterStart(): Unit = {
    SessionFactory.concreteFactory = Some(
      () =>
        Session.create(
          DriverManager.getConnection(container.jdbcUrl, container.username, container.password),
          new PostgreSqlAdapter
      ))
  }
}
