package nz.net.wand.streamevmon

import nz.net.wand.streamevmon.connectors.PostgresConnection

import java.sql.DriverManager

import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import org.apache.flink.api.java.utils.ParameterTool
import org.squeryl.{Session, SessionFactory}
import org.squeryl.adapters.PostgreSqlAdapter

trait PostgresContainerSpec extends TestBase with ForAllTestContainer {

  override val container: PostgreSQLContainer = PostgreSQLContainer("postgres:10")
    .configure(db => {
      val params = ParameterTool.fromPropertiesFile(
        getClass.getClassLoader.getResourceAsStream("default.properties"))

      db.withUsername(params.get("postgres.dataSource.user"))
      db.withPassword(params.get("postgres.dataSource.password"))
      db.withDatabaseName(params.get("postgres.dataSource.databaseName"))

      db.withInitScript("nntsc.sql")

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
          "echo 'Reloaded config' > /has-reloaded"
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

  protected def getPostgres: PostgresConnection = {
    PostgresConnection(
      container.jdbcUrl,
      container.username,
      container.password,
      caching_ttl = 0
    )
  }
}
