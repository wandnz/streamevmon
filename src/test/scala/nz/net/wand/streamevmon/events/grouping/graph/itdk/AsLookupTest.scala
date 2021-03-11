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

class AsLookupTest extends TestBase {

  val asFilename = "itdk-reduced.nodes.as"

  def asFile = getClass.getClassLoader.getResourceAsStream(asFilename)

  "ITDK AsLookup" should {
    "find values matching the provided node ID" when {
      "the value exists" in {
        val tempDir = Files.createTempDirectory(getClass.getCanonicalName)
        try {
          Files.copy(asFile, tempDir.resolve(asFilename))

          val lookup = new ItdkAsLookup(tempDir.resolve(asFilename).toFile)
          Files
            .readAllLines(tempDir.resolve(asFilename))
            .asScala
            .dropWhile(_.startsWith("#"))
            .map(ItdkAsNumber(_))
            .foreach { as =>
              lookup.getAsNumberByNode(as.nodeId.toInt) shouldBe Some(as)
            }
        }
        finally {
          FileUtils.deleteDirectory(tempDir.toFile)
        }
      }

      "the value does not exist" in {
        val tempDir = Files.createTempDirectory(getClass.getCanonicalName)
        try {
          Files.copy(asFile, tempDir.resolve(asFilename))

          val lookup = new ItdkAsLookup(tempDir.resolve(asFilename).toFile)
          val unwantedIds = Files
            .readAllLines(tempDir.resolve(asFilename))
            .asScala
            .dropWhile(_.startsWith("#"))
            .map(ItdkAsNumber(_).nodeId)
            .toSet

          (
            Range(0, 40).filterNot(unwantedIds(_)) ++
              Range(104690, 104800).filterNot(unwantedIds(_))
            ).foreach { id =>
            lookup.getAsNumberByNode(id) shouldBe None
          }
        }
        finally {
          FileUtils.deleteDirectory(tempDir.toFile)
        }
      }
    }
  }
}
