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

package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.connectors.postgres.schema.AsNumber
import nz.net.wand.streamevmon.events.grouping.graph.impl.Host
import nz.net.wand.streamevmon.test.TestBase

import java.net.InetAddress

class HostConstructionTest extends TestBase {
  "Hosts" should {
    "be invalid" when {
      "ampTracerouteUids is non-empty" when {
        "one of hostnames or addresses is non-empty" in {
          an[IllegalArgumentException] shouldBe thrownBy {
            Host(
              Set("wand.net.nz"),
              Set(),
              Set((1, 1, 1)),
              None
            )
          }

          an[IllegalArgumentException] shouldBe thrownBy {
            Host(
              Set(),
              Set((InetAddress.getByName("192.168.0.1"), AsNumber.PrivateAddress)),
              Set((1, 1, 1)),
              None
            )
          }
        }
      }

      // this is the "when all elements are empty" case
      "ampTracerouteUids is empty" when {
        "hostnames and addresses are both empty" in {
          an[IllegalArgumentException] shouldBe thrownBy {
            Host(
              Set(),
              Set(),
              Set(),
              None
            )
          }
        }
      }

      "itdkNodeId is non-empty" when {
        "hostnames and addresses are both empty" in {
          an[IllegalArgumentException] shouldBe thrownBy {
            Host(
              Set(),
              Set(),
              Set(),
              Some((1, AsNumber(1)))
            )
          }
        }
      }
    }

    "be valid" when {
      "ampTracerouteUids is non-empty" when {
        "hostnames and addresses are both empty" in {
          noException shouldBe thrownBy {
            Host(
              Set(),
              Set(),
              Set((1, 1, 1)),
              None
            )
          }
        }
      }

      "itdkNodeId is empty" when {
        "at least one of hostnames or addresses is non-empty" in {
          noException shouldBe thrownBy {
            Host(
              Set("wand.net.nz"),
              Set(),
              Set(),
              None
            )
          }
          noException shouldBe thrownBy {
            Host(
              Set(),
              Set((InetAddress.getByName("192.168.0.1"), AsNumber.PrivateAddress)),
              Set(),
              None
            )
          }
          noException shouldBe thrownBy {
            Host(
              Set("wand.net.nz"),
              Set((InetAddress.getByName("192.168.0.1"), AsNumber.PrivateAddress)),
              Set(),
              None
            )
          }
        }
      }

      "itdkNodeId is non-empty" when {
        "at least one of hostnames or addresses is non-empty" in {
          noException shouldBe thrownBy {
            Host(
              Set("wand.net.nz"),
              Set(),
              Set(),
              Some((1, AsNumber(1)))
            )
          }
          noException shouldBe thrownBy {
            Host(
              Set(),
              Set((InetAddress.getByName("192.168.0.1"), AsNumber.PrivateAddress)),
              Set(),
              Some((1, AsNumber(1)))
            )
          }
          noException shouldBe thrownBy {
            Host(
              Set("wand.net.nz"),
              Set((InetAddress.getByName("192.168.0.1"), AsNumber.PrivateAddress)),
              Set(),
              Some((1, AsNumber(1)))
            )
          }
        }
      }
    }
  }
}
