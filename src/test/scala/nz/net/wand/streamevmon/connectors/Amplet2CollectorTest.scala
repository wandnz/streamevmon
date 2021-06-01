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

import nz.net.wand.streamevmon.connectors.influx.LineProtocol
import nz.net.wand.streamevmon.measurements.amp2.Amp2Measurement
import nz.net.wand.streamevmon.test.TestBase

import java.util.zip.GZIPInputStream

import scala.collection.mutable
import scala.compat.Platform.EOL
import scala.io.Source

class Amplet2CollectorTest extends TestBase {
  def getEntriesFromErrorLog: Iterator[String] = {
    val file = "amplet2-collector-error.log"

    def stream = getClass.getClassLoader.getResourceAsStream(file)

    val knownLinePart = "ERROR nz.net.wand.streamevmon.flink.sources.AmpMeasurementSourceFunction [] - Entry failed to parse: "

    Source
      .fromInputStream(stream)
      .getLines()
      .map { line =>
        val idx = line.indexOf(knownLinePart)
        line.substring(idx + knownLinePart.length)
      }
  }

  def getEntriesFromExport: Iterator[String] = {
    // Yes, this file is not committed. It's large, unwieldy, and temporary.
    val file = "export.lproto.gz"

    def stream = getClass.getClassLoader.getResourceAsStream(file)

    Source
      .fromInputStream(new GZIPInputStream(stream))
      .getLines()
      .filterNot(_.startsWith("#"))
      .filterNot(_.startsWith("CREATE DATABASE"))
  }

  "Amp2 entries" should {
    "be processed without errors" in {
      val typeMap: mutable.Map[String, Int] = mutable.Map()

      var lastTime = System.nanoTime()

      getEntriesFromExport
        .zipWithIndex
        .foreach { case (line, index) =>
          if (index % 10000 == 0) {
            val time = System.nanoTime()
            println(time - lastTime, typeMap.values.sum, typeMap.toList.sortBy(_._1))
            lastTime = System.nanoTime()
          }
          withClue(s"$line$EOL") {
            val proto = LineProtocol(line)
            val meas = Amp2Measurement.createFromLineProtocol(line)
            meas should not be None

            if (typeMap.contains(meas.get.getClass.getSimpleName)) {
              typeMap.put(meas.get.getClass.getSimpleName, typeMap(meas.get.getClass.getSimpleName) + 1)
            }
            else {
              typeMap.put(meas.get.getClass.getSimpleName, 1)
            }
          }
        }
    }
  }
}
