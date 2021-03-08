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
import org.scalatest.BeforeAndAfter

import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class LossyInfluxHistoryTest extends InfluxContainerSpec with BeforeAndAfter {

  "InfluxIO" should {
    "write and read lossy data" when {
      before {
        Await
          .result(
            InfluxIO(containerAddress, containerPort, Some(InfluxCredentials(container.username, container.password)))
              .database(container.database)
              .bulkWriteNative(
                Seq(
                  SeedData.icmp.subscriptionLine,
                  SeedData.icmp.lossySubscriptionLine,
                  SeedData.dns.subscriptionLine,
                  SeedData.dns.lossySubscriptionLine,
                  SeedData.tcpping.subscriptionLine,
                  SeedData.tcpping.lossySubscriptionLine,
                  SeedData.http.subscriptionLine,
                  SeedData.http.lossySubscriptionLine,
                )),
            Duration.Inf
          ) should not be a[Throwable]
      }

      "icmp" in {
        getInfluxHistory.getIcmpData().toList shouldBe List(SeedData.icmp.expected, SeedData.icmp.lossyExpected)
      }

      "dns" in {
        getInfluxHistory.getDnsData().toList shouldBe List(SeedData.dns.expected, SeedData.dns.lossyExpected)
      }

      "tcpping" in {
        getInfluxHistory.getTcppingData().toList shouldBe List(SeedData.tcpping.expected, SeedData.tcpping.lossyExpected)
      }

      "http" in {
        getInfluxHistory.getHttpData().toList shouldBe List(SeedData.http.expected, SeedData.http.lossyExpected)
      }
    }
  }
}
