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

import nz.net.wand.streamevmon.connectors.postgres.schema.AsNumber
import nz.net.wand.streamevmon.test.TestBase

import java.nio.file.{Files, Path}

import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator

import scala.collection.JavaConverters._

//noinspection RegExpRepeatedSpace
class AliasLookupTest extends TestBase {
  var tempDir: Path = _

  val nodesFilename = "itdk-reduced.nodes"
  val asFilename = "itdk-reduced.nodes.as"

  def nodesFile = getClass.getClassLoader.getResourceAsStream(nodesFilename)

  def asFile = getClass.getClassLoader.getResourceAsStream(asFilename)

  var aliasLookup: ItdkAliasLookup = _
  var asLookup: ItdkAsLookup = _

  before {
    tempDir = Files.createTempDirectory(getClass.getCanonicalName)
    Files.copy(nodesFile, tempDir.resolve(nodesFilename))
    Files.copy(asFile, tempDir.resolve(asFilename))

    val preprocessed = ItdkLookupPreprocessor.preprocess(tempDir.resolve(nodesFilename).toFile)

    aliasLookup = new ItdkAliasLookup(preprocessed._1, preprocessed._2)
    asLookup = new ItdkAsLookup(tempDir.resolve(asFilename).toFile)

    Configurator.setLevel(classOf[ItdkAsLookup].getName, Level.INFO)
    Configurator.setLevel(classOf[ItdkAliasLookup].getName, Level.INFO)
  }

  after {
    Option(tempDir).foreach(p => FileUtils.deleteDirectory(p.toFile))
  }

  "ITDK AliasLookup" should {
    "locate the correct details" in {
      // We just test every IP in our example files.
      Files
        .readAllLines(tempDir.resolve(nodesFilename))
        .asScala
        .dropWhile(_.startsWith("#"))
        .foreach { line =>
          val split = line.split(":  ")
          val nodeId = split.head.drop(6).toInt
          val addresses = split.drop(1).head.split(" ")
          addresses.foreach { address =>
            val asn = asLookup
              .getAsNumberByNode(nodeId)
              .getOrElse(AsNumber.Unknown)
            aliasLookup.getNodeFromAddress(address) shouldBe Some((nodeId, asn))
          }
        }
    }
  }
}
