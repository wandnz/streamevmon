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

package nz.net.wand.streamevmon.measurements

import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.test.{PostgresContainerSpec, SeedData}

class PostgresMeasurementMetaCreateTest extends PostgresContainerSpec {
  "PostgresConnection" should {
    lazy val pg = getPostgres

    "obtain correct ICMPMeta" in {
      pg.getMeta(SeedData.icmp.expected) shouldBe Some(SeedData.icmp.expectedMeta)
    }

    "obtain correct DNSMeta" in {
      pg.getMeta(SeedData.dns.expected) shouldBe Some(SeedData.dns.expectedMeta)
    }

    "obtain correct TracerouteMeta" in {
      pg.getMeta(SeedData.traceroutePathlen.expected) shouldBe Some(SeedData.traceroutePathlen.expectedMeta)
    }

    "obtain correct TCPPingMeta" in {
      pg.getMeta(SeedData.tcpping.expected) shouldBe Some(SeedData.tcpping.expectedMeta)
    }

    "obtain correct HTTPMeta" in {
      pg.getMeta(SeedData.http.expected) shouldBe Some(SeedData.http.expectedMeta)
    }

    "obtain several correct Meta objects" in {
      Seq(
        pg.getMeta(SeedData.icmp.expected),
        pg.getMeta(SeedData.dns.expected),
        pg.getMeta(SeedData.traceroutePathlen.expected),
        pg.getMeta(SeedData.tcpping.expected),
        pg.getMeta(SeedData.http.expected)
      ).foreach {
        case Some(x) =>
          x match {
            case _: ICMPMeta => x shouldBe SeedData.icmp.expectedMeta
            case _: DNSMeta => x shouldBe SeedData.dns.expectedMeta
            case _: TracerouteMeta => x shouldBe SeedData.traceroutePathlen.expectedMeta
            case _: TCPPingMeta => x shouldBe SeedData.tcpping.expectedMeta
            case _: HTTPMeta => x shouldBe SeedData.http.expectedMeta
            case _                 => fail("Created a type we didn't recognise")
          }
        case None => fail("Failed to create an object")
      }
    }
  }
}
