/* This file is part of streamevmon.
 *
 * Copyright (C) 2021  The University of Waikato, Hamilton, New Zealand
 *
 * Author: Daniel Oosterwijk
 *
 * All rights reserved.
 *
 * This code has been developed by the University of Waikato WAND
 * research group. For further information please see https://wand.nz,
 * or our Github organisation at https://github.com/wanduow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.net.wand.streamevmon.test

import nz.net.wand.streamevmon.connectors.postgres.PostgresConnection
import nz.net.wand.streamevmon.Configuration

import java.sql.DriverManager

import com.dimafeng.testcontainers.PostgreSQLContainer
import org.squeryl.{Session, SessionFactory}
import org.squeryl.adapters.PostgreSqlAdapter
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName

abstract class PostgresContainerSpec extends TaggedForAllTestContainer with TestBase {

  // We know that NNTSC uses postgres 10, so we might as well stick with
  // that version. Alpine for size benefits.
  override val container: PostgreSQLContainer = PostgreSQLContainer(
    DockerImageName.parse("postgres").withTag("10-alpine")
  )
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
      db.waitingFor(new HostPortWaitStrategy())
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
