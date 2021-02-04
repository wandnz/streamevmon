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

package nz.net.wand.streamevmon.connectors

import nz.net.wand.streamevmon.{PostgresContainerSpec, SeedData}
import nz.net.wand.streamevmon.connectors.postgres.PostgresConnection

import java.sql.DriverManager

class PostgresConnectionTest extends PostgresContainerSpec {
  "PostgreSQL container" should {
    "successfully connect with raw JDBC" in {
      val connection =
        DriverManager.getConnection(container.jdbcUrl, container.username, container.password)
      connection.isValid(0) shouldBe true
      connection.close()
    }

    "contain expected metadata" in {
      import nz.net.wand.streamevmon.connectors.postgres.PostgresSchema._
      import nz.net.wand.streamevmon.connectors.postgres.SquerylEntrypoint._

      transaction(icmpMeta.allRows.toList) shouldBe SeedData.icmp.allExpectedMeta
      transaction(dnsMeta.allRows.toList) shouldBe SeedData.dns.allExpectedMeta
      transaction(tracerouteMeta.allRows.toList) shouldBe SeedData.traceroute.allExpectedMeta
    }
  }

  "PostgresConnection" should {
    def getConn = PostgresConnection(container.jdbcUrl, container.username, container.password, 0)

    "retrieve all TracerouteMeta entries" in {
      getConn.getAllTracerouteMeta shouldBe Some(SeedData.traceroute.allExpectedMeta)
    }

    "retrieve all TraceroutePath entries" in {
      val data = getConn.getAllTraceroutePaths(18).get
      data should contain(SeedData.traceroute.expectedPath)
      data should have size 21
    }

    "retrieve all TracerouteAsPath entries" in {
      val data = getConn.getAllTracerouteAsPaths(18).get
      data should contain(SeedData.traceroute.expectedAsPath)
      data should have size 6
    }

    "retrieve an expected Traceroute entry" in {
      val data = getConn.getTracerouteData(18).get
      data should contain(SeedData.traceroute.expected)
      data should have size 405
    }

    "retrieve the expected TraceroutePath entry" in {
      getConn.getTraceroutePath(getConn.getTracerouteData(18).get.head) shouldBe Some(
        SeedData.traceroute.expectedPath)
    }

    "retrieve the expected TracerouteAsPath entry" in {
      getConn.getTracerouteAsPath(getConn.getTracerouteData(18).get.head) shouldBe Some(
        SeedData.traceroute.expectedAsPath)
    }
  }
}
