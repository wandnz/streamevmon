/* This file is part of streamevmon.
 *
 * Copyright (C) 2021  The University of Waikato, Hamilton, New Zealand
 *
 * Author: <unknown>
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

import nz.net.wand.streamevmon.connectors.postgres.schema.{AsNumber, AsNumberCategory}
import nz.net.wand.streamevmon.test.TestBase

import java.io.{File, FileReader, RandomAccessFile}
import java.net.InetAddress
import java.nio.file.Files
import java.nio.ByteBuffer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.Level
import org.scalatest.GivenWhenThen

import scala.collection.JavaConverters._

//noinspection RegExpRepeatedSpace
class PreprocessorTest extends TestBase with GivenWhenThen {

  val nodesFilename = "itdk-reduced.nodes"
  val asFilename = "itdk-reduced.nodes.as"

  def nodesFile = getClass.getClassLoader.getResourceAsStream(nodesFilename)

  def asFile = getClass.getClassLoader.getResourceAsStream(asFilename)

  def testAlignedFile(
    alignedReader: RandomAccessFile,
    originalLines: Iterable[String],
    asLookup: ItdkAsLookup,
    entryLength  : Int = 12,
  ): Unit = {
    val buf = Array.ofDim[Byte](entryLength)

    Range(0, alignedReader.length.toInt / entryLength - 1)
      .foreach { index =>
        alignedReader.seek(index * entryLength)
        alignedReader.readFully(buf)
        val address = InetAddress.getByAddress(buf.take(4)).getHostAddress
        // yeah, it's weird to test a space on both sides like this, but I'm not sure
        // of a graceful way to take advantage of string search optimisations and also
        // compress this into a single `l.split(" ").contains(address)
        originalLines.find(_.split(' ').contains(address)) match {
          case None => fail("Aligned file contained an address that wasn't in the original file")
          case Some(originalLine) =>
            val nodeId = originalLine.split(":  ").head.drop(6).toInt
            // Ensure the aligned file had the right node ID
            ByteBuffer.wrap(buf.drop(4)).getInt shouldBe nodeId
            // and the right AS number
            AsNumber(ByteBuffer.wrap(buf.drop(8)).getInt) shouldBe
              asLookup.getAsNumberByNode(nodeId).getOrElse(AsNumber.Unknown)
        }
      }
  }

  def testLookupMap(
    lookupMapFile: File
  ): Unit = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    val jsonIn = new FileReader(lookupMapFile)
    val lookupMap = mapper
      .readValue(jsonIn, classOf[Map[String, Double]])
      .map(kv => (kv._1.toInt.toByte, kv._2))

    lookupMap.keys should have size 256
    every(lookupMap.values) should (be >= 0.0 and be <= 1.0)
  }

  "ITDK preprocessor" should {

    "create a valid file" when {
      "using individual steps" in {
        Given("a working environment")
        val tempDir = Files.createTempDirectory(getClass.getCanonicalName)
        Files.copy(nodesFile, tempDir.resolve(nodesFilename))
        Files.copy(asFile, tempDir.resolve(asFilename))

        // We're not testing this class right now, but it's much easier to use
        // it than to reimplement its functionality for this test.
        val asLookup = new ItdkAsLookup(tempDir.resolve(asFilename).toFile)
        // Quiet down the logging, since the lookup produces a lot of messages
        // at TRACE level.
        Configurator.setLevel(classOf[ItdkAsLookup].getName, Level.INFO)

        try {
          When("the nodes file is inverted")
          val invertedFile = ItdkLookupPreprocessor.invertItdkNodeLookup(
            tempDir.resolve(nodesFilename).toFile,
            tempDir.resolve(asFilename).toFile
          )
          val invertedLines = Files.readAllLines(invertedFile.toPath).asScala

          Then("the inverted file should have the same content")
          // We test that all addresses in the input file are present in the
          // inverted file. They also must have the correct node ID and ASN.
          Files
            .readAllLines(tempDir.resolve(nodesFilename))
            .asScala
            .dropWhile(_.startsWith("#"))
            .foreach { inputLine =>
              val split = inputLine.split(":  ")
              val nodeId = split.head.drop(6)
              val addresses = split.drop(1).head.split(" ")
              addresses.foreach { address =>
                invertedLines.find(_.startsWith(s"$address ")) match {
                  case Some(resultLine) =>
                    val asn = asLookup
                      .getAsNumberByNode(nodeId.toInt)
                      .flatMap(_.number)
                      .getOrElse(AsNumberCategory.Unknown.id)
                    resultLine shouldBe s"$address $nodeId $asn"
                  case None => fail(s"Output file did not include $address from N$nodeId")
                }
              }
            }

          When("the inverted file is sorted")
          val sortedFile = ItdkLookupPreprocessor.sortFile(invertedFile)
          val sortedLines = Files.readAllLines(sortedFile.toPath)

          Then("the sorted file must have the same lines as the inverted file")
          Files
            .readAllLines(invertedFile.toPath).asScala
            .foreach(line => assert(sortedLines.contains(line)))

          When("the aligned file is created")
          val alignedFile = ItdkLookupPreprocessor.createAlignedInvertedMapFile(sortedFile)
          val alignedReader = new RandomAccessFile(alignedFile._1, "r")

          Then("the aligned file must have the same content as the original file")
          // We could use an ItdkAliasLookup here, but we're not testing that
          // class. Since we just read every entry sequentially, we don't need
          // to perform lookup, so we don't need to use the class.
          val originalLines = Files
            .readAllLines(tempDir.resolve(nodesFilename))
            .asScala

          testAlignedFile(
            alignedReader,
            originalLines,
            asLookup
          )

          And("the lookup map must be of a valid format")
          testLookupMap(alignedFile._2)
        }
        finally {
          FileUtils.deleteDirectory(tempDir.toFile)
        }
      }

      "using a single step" in {
        val tempDir = Files.createTempDirectory(getClass.getCanonicalName)
        try {
          Files.copy(nodesFile, tempDir.resolve(nodesFilename))
          Files.copy(asFile, tempDir.resolve(asFilename))
          val asLookup = new ItdkAsLookup(tempDir.resolve(asFilename).toFile)
          Configurator.setLevel(classOf[ItdkAsLookup].getName, Level.INFO)

          val result = ItdkLookupPreprocessor.preprocess(tempDir.resolve(nodesFilename).toFile)
          testAlignedFile(
            new RandomAccessFile(result._1, "r"),
            Files.readAllLines(tempDir.resolve(nodesFilename)).asScala,
            asLookup
          )
          testLookupMap(result._2)
        }
        finally {
          FileUtils.deleteDirectory(tempDir.toFile)
        }
      }
    }

    "cleanup intermediate files" in {
      val tempDir = Files.createTempDirectory(getClass.getCanonicalName)
      try {
        Files.copy(nodesFile, tempDir.resolve(nodesFilename))
        Files.copy(asFile, tempDir.resolve(asFilename))
        ItdkLookupPreprocessor.preprocess(tempDir.resolve(nodesFilename).toFile, cleanup = true)
        Files.list(tempDir).forEach { f =>
          f.toString should (not endWith ".inverted" and not endWith ".inverted.sorted")
        }
      }
      finally {
        FileUtils.deleteDirectory(tempDir.toFile)
      }
    }

    "not cleanup intermediate files" in {
      val tempDir = Files.createTempDirectory(getClass.getCanonicalName)
      try {
        Files.copy(nodesFile, tempDir.resolve(nodesFilename))
        Files.copy(asFile, tempDir.resolve(asFilename))
        ItdkLookupPreprocessor.preprocess(tempDir.resolve(nodesFilename).toFile)

        assert(Files.list(tempDir).anyMatch(_.toString.endsWith(".inverted")))
        assert(Files.list(tempDir).anyMatch(_.toString.endsWith(".inverted.sorted")))
      }
      finally {
        FileUtils.deleteDirectory(tempDir.toFile)
      }
    }
  }
}
