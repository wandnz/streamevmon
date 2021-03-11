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

class GeoLookupTest extends TestBase {

  val geoFilename = "itdk-reduced.nodes.geo"

  def geoFile = getClass.getClassLoader.getResourceAsStream(geoFilename)

  "ITDK GeoLookup" should {
    "find values matching the provided node ID" when {
      "the value exists" in {
        val tempDir = Files.createTempDirectory(getClass.getCanonicalName)
        try {
          Files.copy(geoFile, tempDir.resolve(geoFilename))

          val lookup = new ItdkGeoLookup(tempDir.resolve(geoFilename).toFile)
          Files
            .readAllLines(tempDir.resolve(geoFilename))
            .asScala
            .dropWhile(_.startsWith("#"))
            .map(GeoInfo(_))
            .foreach { geo =>
              lookup.getGeoInfoByNode(geo.nodeId.toInt) shouldBe Some(geo)
            }
        }
        finally {
          FileUtils.deleteDirectory(tempDir.toFile)
        }
      }

      "the value does not exist" in {
        val tempDir = Files.createTempDirectory(getClass.getCanonicalName)
        try {
          Files.copy(geoFile, tempDir.resolve(geoFilename))

          val lookup = new ItdkGeoLookup(tempDir.resolve(geoFilename).toFile)
          val unwantedIds = Files
            .readAllLines(tempDir.resolve(geoFilename))
            .asScala
            .dropWhile(_.startsWith("#"))
            .map(GeoInfo(_).nodeId)
            .toSet

          (
            Range(0, 40).filterNot(unwantedIds(_)) ++
              Range(104690, 104800).filterNot(unwantedIds(_))
            ).foreach { id =>
            lookup.getGeoInfoByNode(id) shouldBe None
          }
        }
        finally {
          FileUtils.deleteDirectory(tempDir.toFile)
        }
      }
    }
  }
}
