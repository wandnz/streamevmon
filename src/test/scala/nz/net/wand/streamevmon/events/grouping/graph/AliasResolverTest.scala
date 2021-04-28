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

package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.connectors.postgres.schema.AsNumber
import nz.net.wand.streamevmon.events.grouping.graph.itdk.{ItdkAliasLookup, ItdkAsLookup, ItdkLookupPreprocessor}
import nz.net.wand.streamevmon.events.grouping.graph.pruning.AliasResolver
import nz.net.wand.streamevmon.test.TestBase

import java.io.{BufferedReader, InputStreamReader}
import java.net.InetAddress
import java.nio.file.{Files, Path}

import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.scalatest.exceptions.TestFailedException

import scala.collection.JavaConverters._
import scala.collection.mutable

//noinspection RegExpRepeatedSpace
class AliasResolverTest extends TestBase {
  val nodesFilename = "itdk-reduced.nodes"
  val asFilename = "itdk-reduced.nodes.as"

  def nodesFile = getClass.getClassLoader.getResourceAsStream(nodesFilename)

  def asFile = getClass.getClassLoader.getResourceAsStream(asFilename)

  def setupItdk: (Path, ItdkAliasLookup, ItdkAsLookup) = {
    val tempDir: Path = Files.createTempDirectory(getClass.getCanonicalName)

    Files.copy(nodesFile, tempDir.resolve(nodesFilename))
    Files.copy(asFile, tempDir.resolve(asFilename))

    val preprocessed = ItdkLookupPreprocessor.preprocess(tempDir.resolve(nodesFilename).toFile)

    val aliasLookup = new ItdkAliasLookup(preprocessed._1, preprocessed._2)
    val asLookup = new ItdkAsLookup(tempDir.resolve(asFilename).toFile)

    Configurator.setLevel(classOf[ItdkAsLookup].getName, Level.INFO)
    Configurator.setLevel(classOf[ItdkAliasLookup].getName, Level.INFO)

    (tempDir, aliasLookup, asLookup)
  }

  type HostT = AliasResolver#HostT
  val itdkHosts: Iterable[HostT] = {
    def selectHostnames(nodeId: Int, groupIndex: Int): Set[String] = {
      // A few nodes will have no hostnames attached
      if (nodeId % 5 == 0) {
        Set()
      }
      // Some groups of addresses in each node will have a different hostname
      else if (groupIndex % 2 == 0) {
        Set(s"group-$groupIndex.node-$nodeId.itdk.example.org")
      }
      // The rest of the entries will just use their node as a hostname
      else {
        Set(s"node-$nodeId.itdk.example.org")
      }
    }

    new BufferedReader(new InputStreamReader(nodesFile))
      .lines()
      .iterator().asScala
      .dropWhile(_.startsWith("#"))
      .flatMap { line =>
        val split = line.split(":  ")
        val nodeId = split.head.drop(6).toInt
        val addresses = split.drop(1).head.split(" ")

        addresses
          .sliding(5, 5)
          .zipWithIndex
          .flatMap { case (addressGroup, index) =>
            addressGroup.map { address =>
              new HostT(
                selectHostnames(nodeId, index),
                // The resolver doesn't actually make any use of AS numbers, so
                // we don't need to test with any of them.
                Set((InetAddress.getByName(address), AsNumber.Unknown)),
                // All our test hosts have addresses, so there are no anonymous ones
                Set(),
                // We do want to test that naive resolution works properly when
                // an ITDK dataset is available, so while we start off with no
                // information, these hosts are in the dataset. We also provide
                // a few that aren't.
                None
              )
            }
          }
      }
      .toIterable
  }
  val nonItdkHosts: Iterable[HostT] = Iterable(
    // We also add a few hosts that aren't part of ITDK. We can ensure that
    // by using a private address range.
    new HostT(
      Set("host-a.example.org"),
      Set((InetAddress.getByName("10.0.0.1"), AsNumber.PrivateAddress)),
      Set(),
      None
    ),
    new HostT(
      Set("host-a.example.org"),
      Set((InetAddress.getByName("10.0.0.2"), AsNumber.PrivateAddress)),
      Set(),
      None
    ),
    new HostT(
      Set("host-a.example.org"),
      Set((InetAddress.getByName("192.168.0.123"), AsNumber.PrivateAddress)),
      Set(),
      None
    ),
    new HostT(
      Set("host-b.example.org"),
      Set((InetAddress.getByName("10.0.10.0"), AsNumber.PrivateAddress)),
      Set(),
      None
    ),
    new HostT(
      Set("host-b.example.org"),
      Set((InetAddress.getByName("10.0.10.1"), AsNumber.PrivateAddress)),
      Set(),
      None
    ),
    new HostT(
      Set(),
      Set((InetAddress.getByName("172.16.0.1"), AsNumber.PrivateAddress)),
      Set(),
      None
    )
  )
  val allHosts: Iterable[HostT] = itdkHosts ++ nonItdkHosts

  "AliasResolver" should {
    "perform naive resolution" when {
      def testNaiveResolution(resolver: AliasResolver): Unit = {
        // Naive resolution simply merges hosts with the same hostname.
        val seenHostnames = mutable.Set[String]()
        val onNewHost = { host: HostT =>
          if (host.hostnames.exists(seenHostnames.contains)) {
            fail(s"Hostnames not unique: $host")
          }
        }
        val onUpdateHost: (HostT, HostT) => Unit = {
          case (host, host2) =>
            if (!host.sharesHostnamesWith(host2)) {
              fail(s"Hostnames not shared: $host, $host2")
            }
        }

        nonItdkHosts.foreach(h => resolver.resolve(h, onNewHost, onUpdateHost))
      }

      "an ITDK dataset is not provided" in {
        testNaiveResolution(new AliasResolver(None))
      }

      "an ITDK dataset is provided" in {
        val (tempDir, aliasLookup, _) = setupItdk
        try {
          testNaiveResolution(new AliasResolver(Some(aliasLookup)))
        }
        finally {
          FileUtils.deleteDirectory(tempDir.toFile)
        }
      }
    }

    /** I can't figure out a good way to determine that ITDK merging has
      * actually occurred. Until this changes, we're just going to stick with
      * a simple assertion that some ITDK node ID has been assigned.
      */
    def testItdkResolution(resolver: AliasResolver): Unit = {
      val seenHostnames = mutable.Set[String]()
      val onNewHost = { host: HostT =>
        if (host.hostnames.exists(seenHostnames.contains)) {
          fail(s"Hostnames not unique: $host")
        }
      }
      val onUpdateHost: (HostT, HostT) => Unit = {
        case (host, host2) =>
          if (!host.sharesHostnamesWith(host2) && !host.equalItdkNode(host2)) {
            fail(s"Hostnames not shared and no ITDK node in common: $host, $host2")
          }
      }

      allHosts.foreach(h => resolver.resolve(h, onNewHost, onUpdateHost))
      val afterMerge = resolver.mergedHosts

      assert(afterMerge.exists(_._2.itdkNodeId.isDefined))
    }

    "perform ITDK resolution" when {
      "an ITDK dataset is provided" in {
        val (tempDir, aliasLookup, _) = setupItdk
        try {
          noException shouldBe thrownBy(
            testItdkResolution(new AliasResolver(Some(aliasLookup)))
          )
        }
        finally {
          FileUtils.deleteDirectory(tempDir.toFile)
        }
      }
    }

    "not perform ITDK resolution" when {
      "an ITDK dataset is not provided" in {
        a[TestFailedException] shouldBe thrownBy(
          testItdkResolution(new AliasResolver(None))
        )
      }
    }
  }
}
