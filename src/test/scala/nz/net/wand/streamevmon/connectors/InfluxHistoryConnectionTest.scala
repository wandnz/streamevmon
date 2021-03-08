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

import nz.net.wand.streamevmon.test.{InfluxContainerSpec, SeedData}

import com.github.fsanaulla.chronicler.ahc.io.InfluxIO
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class InfluxHistoryConnectionTest extends InfluxContainerSpec {
  "InfluxDB container" should {
    "successfully ping" in {
      val influx =
        InfluxIO(containerAddress, containerPort, Some(InfluxCredentials(container.username, container.password)))

      Await.result(influx.ping.map {
        case Right(_) => succeed
        case Left(_)  => fail
      }, Duration.Inf)
    }
  }

  "InfluxHistoryConnection" should {
    before {
      Await.result(
        InfluxIO(containerAddress, containerPort, Some(InfluxCredentials(container.username, container.password)))
          .database(container.database)
          .bulkWriteNative(
            Seq(
              SeedData.icmp.subscriptionLine,
              SeedData.dns.subscriptionLine,
              SeedData.traceroutePathlen.subscriptionLine,
              SeedData.http.subscriptionLine,
              SeedData.tcpping.subscriptionLine
            ) ++ SeedData.bigdata.flowsAsLineProtocol
          ),
        Duration.Inf
      )
    }

    "get ICMP data" in {
      getInfluxHistory.getIcmpData().next shouldBe SeedData.icmp.expected
    }

    "get DNS data" in {
      getInfluxHistory.getDnsData().next shouldBe SeedData.dns.expected
    }

    "get HTTP data" in {
      getInfluxHistory.getHttpData().next shouldBe SeedData.http.expected
    }

    "get TCPPing data" in {
      getInfluxHistory.getTcppingData().next shouldBe SeedData.tcpping.expected
    }

    "get Traceroute data" in {
      getInfluxHistory.getTracerouteData().next shouldBe SeedData.traceroutePathlen.expected
    }

    "get Flow data" in {
      getInfluxHistory.getFlowStatistics().toSeq shouldBe SeedData.bigdata.flowsExpected
    }

    "get data between a time range" in {
      val conn = getInfluxHistory
      conn.getIcmpData(end = SeedData.icmp.expected.time.minusSeconds(10)).toSeq shouldBe Seq()
      conn.getIcmpData(start = SeedData.icmp.expected.time.plusSeconds(10)).toSeq shouldBe Seq()
      conn
        .getIcmpData(
          start = SeedData.icmp.expected.time.minusSeconds(10),
          end = SeedData.icmp.expected.time.plusSeconds(10)
        )
        .next shouldBe SeedData.icmp.expected
    }

    "get all data" in {
      getInfluxHistory.getAllAmpData().toSeq shouldBe Seq(
        SeedData.icmp.expected,
        SeedData.dns.expected,
        SeedData.http.expected,
        SeedData.tcpping.expected,
        SeedData.traceroutePathlen.expected
      )
    }
  }
}
