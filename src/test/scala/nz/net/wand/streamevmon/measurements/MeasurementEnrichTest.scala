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

import nz.net.wand.streamevmon.{PostgresContainerSpec, SeedData}
import nz.net.wand.streamevmon.measurements.traits.InfluxMeasurementFactory

class MeasurementEnrichTest extends PostgresContainerSpec {
  "Children of Measurement.enrich" should {

    lazy val pg = getPostgres

    "obtain the correct RichICMP object" in {
      InfluxMeasurementFactory.enrichMeasurement(pg, SeedData.icmp.expected) shouldBe Some(SeedData.icmp.expectedRich)
    }

    "obtain the correct RichDNS object" in {
      InfluxMeasurementFactory.enrichMeasurement(pg, SeedData.dns.expected) shouldBe Some(SeedData.dns.expectedRich)
    }

    "obtain the correct RichTraceroutePathlen object" in {
      InfluxMeasurementFactory.enrichMeasurement(pg, SeedData.traceroutePathlen.expected) shouldBe Some(SeedData.traceroutePathlen.expectedRich)
    }

    "obtain the correct RichTcpping object" in {
      InfluxMeasurementFactory.enrichMeasurement(pg, SeedData.tcpping.expected) shouldBe Some(SeedData.tcpping.expectedRich)
    }

    "obtain the correct RichHTTP object" in {
      InfluxMeasurementFactory.enrichMeasurement(pg, SeedData.http.expected) shouldBe Some(SeedData.http.expectedRich)
    }
  }
}
