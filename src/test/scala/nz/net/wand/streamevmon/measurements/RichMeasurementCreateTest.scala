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

import nz.net.wand.streamevmon.{SeedData, TestBase}
import nz.net.wand.streamevmon.measurements.amp._

class RichMeasurementCreateTest extends TestBase {
  "Children of RichMeasurement.create" should {
    "merge an ICMP and ICMPMeta object" in {
      RichICMP.create(SeedData.icmp.expected, SeedData.icmp.expectedMeta) shouldBe Some(SeedData.icmp.expectedRich)
    }

    "merge a DNS and DNSMeta object" in {
      RichDNS.create(SeedData.dns.expected, SeedData.dns.expectedMeta) shouldBe Some(SeedData.dns.expectedRich)
    }

    "merge a TraceroutePathlen and TracerouteMeta object" in {
      RichTraceroutePathlen.create(SeedData.traceroutePathlen.expected, SeedData.traceroutePathlen.expectedMeta) shouldBe Some(SeedData.traceroutePathlen.expectedRich)
    }

    "merge a TCPPing and TCPPingMeta object" in {
      RichTCPPing.create(SeedData.tcpping.expected, SeedData.tcpping.expectedMeta) shouldBe Some(SeedData.tcpping.expectedRich)
    }

    "merge an HTTP and HTTPMeta object" in {
      RichHTTP.create(SeedData.http.expected, SeedData.http.expectedMeta) shouldBe Some(SeedData.http.expectedRich)
    }
  }
}
