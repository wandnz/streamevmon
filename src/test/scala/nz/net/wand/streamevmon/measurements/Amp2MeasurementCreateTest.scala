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

import nz.net.wand.streamevmon.measurements.amp2.Amp2Measurement
import nz.net.wand.streamevmon.test.InfluxContainerSpec

import java.util.zip.GZIPInputStream

import com.github.fsanaulla.chronicler.ahc.io.InfluxIO
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials

import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

class Amp2MeasurementCreateTest extends InfluxContainerSpec {

  def getEntriesFromExport: Iterator[String] = {
    Source
      .fromInputStream(new GZIPInputStream(
        getClass.getClassLoader.getResourceAsStream("amp2.lproto.gz")
      ))
      .getLines()
  }

  def testLineProtocolToMeasurement(lines: Iterator[String]): Iterator[Amp2Measurement] = {
    lines.map { line =>
      Amp2Measurement.createFromLineProtocol(line) match {
        case Some(meas) => meas
        case None => fail(s"Did not create measurement from line $line")
      }
    }
  }

  "Amp2 entries" should {
    "be processed without errors" in {
      testLineProtocolToMeasurement(getEntriesFromExport).toList
    }
  }

  "InfluxDB container" should {
    "successfully ping" in {
      val influx =
        InfluxIO(containerAddress, containerPort, Some(InfluxCredentials(container.username, container.password)))

      Await.result(influx.ping.map {
        case Right(_) => succeed
        case Left(_) => fail
      }, Duration.Inf)
    }
  }

  "Amp2InfluxHistoryConnection" should {
    before {
      val influx = InfluxIO(containerAddress, containerPort, Some(InfluxCredentials(container.username, container.password)))

      val items = getEntriesFromExport.toList

      Await.result(influx.ping.map {
        case Right(_) => succeed
        case Left(_) => fail
      }, Duration.Inf)

      val result = items.sliding(items.size / 10, items.size / 10).map { some =>
        Await.result(
          influx.database(container.database).bulkWriteNative(some),
          Duration.Inf
        )
      }.toList

      result should not contain a[Left[_, _]]
    }

    "get data of various types" in {
      val dataFromHistory = getAmp2InfluxHistory.getAllAmp2Data().toSet
      val dataFromLineProto = testLineProtocolToMeasurement(getEntriesFromExport).toSet
      dataFromHistory.diff(dataFromLineProto) shouldBe empty
      dataFromLineProto.diff(dataFromHistory) shouldBe empty
    }
  }
}
