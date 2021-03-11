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

package nz.net.wand.streamevmon.events.grouping.graph.itdk

import nz.net.wand.streamevmon.test.TestBase

import java.nio.file.Files

import org.apache.commons.io.FileUtils

import scala.collection.JavaConverters._

class ItdkDataClassCreateTest extends TestBase {
  "GeoInfo" should {

    val geoFilename = "itdk-reduced.nodes.geo"

    def geoFile = getClass.getClassLoader.getResourceAsStream(geoFilename)

    "be created from .geo lines" in {
      // There's not really a lot to test here since we don't make an
      // effort to validate that the contents are correct after creation,
      // so we'll just make sure it doesn't crash and there's no nulls.
      val tempDir = Files.createTempDirectory(getClass.getCanonicalName)
      try {
        Files.copy(geoFile, tempDir.resolve(geoFilename))

        Files
          .readAllLines(tempDir.resolve(geoFilename))
          .asScala
          .dropWhile(_.startsWith("#"))
          .foreach { l =>
            noException should be thrownBy {
              val g = GeoInfo(l)
              assert(g.continent != null)
              assert(g.country != null)
              assert(g.region != null)
              assert(g.city != null)
            }
          }
      }
      finally {
        FileUtils.deleteDirectory(tempDir.toFile)
      }
    }
  }

  "ItdkAsNumber" should {
    val asFilename = "itdk-reduced.nodes.as"

    def asFile = getClass.getClassLoader.getResourceAsStream(asFilename)

    "be created from observed .as lines" in {
      val tempDir = Files.createTempDirectory(getClass.getCanonicalName)
      try {
        Files.copy(asFile, tempDir.resolve(asFilename))

        Files
          .readAllLines(tempDir.resolve(asFilename))
          .asScala
          .foreach { l =>
            noException should be thrownBy {
              val n = ItdkAsNumber(l)
              n.method should be('defined)
            }
          }
      }
      finally {
        FileUtils.deleteDirectory(tempDir.toFile)
      }
    }

    "not crash when an unknown method is supplied" in {
      noException should be thrownBy {
        val n = ItdkAsNumber("node.AS N12345 54321 mysterious")
        n.method shouldNot be('defined)
      }
    }
  }
}
