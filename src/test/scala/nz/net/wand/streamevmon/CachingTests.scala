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

package nz.net.wand.streamevmon

import nz.net.wand.streamevmon.connectors.postgres.schema.AsInetPath
import nz.net.wand.streamevmon.measurements.amp.ICMPMeta
import nz.net.wand.streamevmon.test.{SeedData, TaggedForEachTestContainer, TestBase}

import java.nio.file.{Files, Path}

import com.dimafeng.testcontainers.GenericContainer
import org.apache.commons.io.FileUtils
import org.apache.flink.api.java.utils.ParameterTool
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.ImageFromDockerfile

import scala.collection.JavaConverters._
import scala.compat.Platform.EOL
import scala.concurrent.duration._

trait CachingBehaviours extends TestBase {

  class CachingProxy extends Caching {
    invalidateAll()

    override def getWithCache[T](key: String, ttl: Option[FiniteDuration], method: => T): T = super.getWithCache(key, ttl, method)
  }

  def getCachingObject: CachingProxy

  /** Implementations of this trait test these behaviours after setting up their
    * caching model. The behaviours are all the same, since the functions being
    * tested should behave identically regardless of backend.
    */
  def cachingBehaviours(): Unit = {
    "return plain results the first time" in {
      val caching = getCachingObject
      caching.getWithCache("primitive", None, 1) shouldBe 1
      caching.getWithCache("option", None, Some(1)) shouldBe Some(1)
      caching.getWithCache("caseClass", None, SeedData.icmp.expectedMeta) shouldBe SeedData.icmp.expectedMeta
      caching.getWithCache("complexObject", None, SeedData.traceroute.expectedAsInetPath) shouldBe SeedData.traceroute.expectedAsInetPath
    }

    "retain results when TTL is None" in {
      val caching = getCachingObject
      caching.getWithCache("primitive", None, 2) shouldBe 2
      caching.getWithCache("option", None, Some(2)) shouldBe Some(2)
      caching.getWithCache("caseClass", None, SeedData.icmp.expectedMeta) shouldBe SeedData.icmp.expectedMeta
      caching.getWithCache("complexObject", None, SeedData.traceroute.expectedAsInetPath) shouldBe SeedData.traceroute.expectedAsInetPath

      caching.getWithCache[Int]("primitive", None, -1) shouldBe 2
      caching.getWithCache[Option[Int]]("option", None, None) shouldBe Some(2)
      caching.getWithCache[ICMPMeta]("caseClass", None, null) shouldBe SeedData.icmp.expectedMeta
      caching.getWithCache[AsInetPath]("complexObject", None, null) shouldBe SeedData.traceroute.expectedAsInetPath
    }

    "retain results when TTL is not None" in {
      val caching = getCachingObject
      caching.getWithCache("primitive", Some(FiniteDuration(1, SECONDS)), 2) shouldBe 2
      caching.getWithCache("option", Some(FiniteDuration(1, SECONDS)), Some(2)) shouldBe Some(2)
      caching.getWithCache("caseClass", None, SeedData.icmp.expectedMeta) shouldBe SeedData.icmp.expectedMeta
      caching.getWithCache("complexObject", Some(FiniteDuration(1, SECONDS)), SeedData.traceroute.expectedAsInetPath) shouldBe SeedData.traceroute.expectedAsInetPath

      caching.getWithCache[Int]("primitive", Some(FiniteDuration(1, SECONDS)), -1) shouldBe 2
      caching.getWithCache[Option[Int]]("option", Some(FiniteDuration(1, SECONDS)), None) shouldBe Some(2)
      caching.getWithCache[ICMPMeta]("caseClass", Some(FiniteDuration(1, SECONDS)), null) shouldBe SeedData.icmp.expectedMeta
      caching.getWithCache[AsInetPath]("complexObject", Some(FiniteDuration(1, SECONDS)), null) shouldBe SeedData.traceroute.expectedAsInetPath
    }

    "expire results when TTL is not None" in {
      val caching = getCachingObject
      caching.getWithCache("primitive", Some(FiniteDuration(1, SECONDS)), 2) shouldBe 2
      caching.getWithCache("option", Some(FiniteDuration(1, SECONDS)), Some(2)) shouldBe Some(2)
      caching.getWithCache("caseClass", Some(FiniteDuration(1, SECONDS)), SeedData.icmp.expectedMeta) shouldBe SeedData.icmp.expectedMeta
      caching.getWithCache("complexObject", Some(FiniteDuration(1, SECONDS)), SeedData.traceroute.expectedAsInetPath) shouldBe SeedData.traceroute.expectedAsInetPath

      Thread.sleep(1100)

      caching.getWithCache[Int]("primitive", Some(FiniteDuration(1, SECONDS)), -1) should not be 2
      caching.getWithCache[Option[Int]]("option", Some(FiniteDuration(1, SECONDS)), None) should not be Some(2)
      caching.getWithCache[ICMPMeta]("caseClass", Some(FiniteDuration(1, SECONDS)), null) should not be SeedData.icmp.expectedMeta
      caching.getWithCache[AsInetPath]("complexObject", Some(FiniteDuration(1, SECONDS)), null) should not be SeedData.traceroute.expectedAsInetPath
    }
  }
}

/** The simple case for caching tests. Caffeine cache is in-memory.
  */
class CachingCaffeineBackendTest extends CachingBehaviours {
  override def getCachingObject = new CachingProxy()

  "Caffeine cache backend" should {
    behave like cachingBehaviours
  }
}

/** A more complicated case for caching tests. We require an external Memcached
  * server, so we set one up with Docker. We had trouble getting everything to
  * work correctly using the plain image, so we build one that supports docker
  * healthchecks so that we can wait for the server to be properly ready before
  * performing tests.
  *
  * Using a ForEachTestContainer does mean that the tests take a little longer
  * than usual, but we need to make sure that the tests don't run concurrently
  * or on the same instance.
  */
class CachingMemcachedBackendTest
  extends TaggedForEachTestContainer
          with CachingBehaviours {

  /** Docker needs a directory to use as the build context. */
  val tempDir = Files.createTempDirectory(Path.of("/tmp"), "memcached-Docker")

  // We add a real Dockerfile to the temp directory we created and let
  // Testcontainers tell Docker to build it.
  override val container = {
    reflect.io.File(tempDir + "/Dockerfile").writeAll {
      "FROM memcached:alpine" + EOL +
        "USER root" + EOL +
        "RUN apk add --update netcat-openbsd && rm -rf /var/cache/apk/*" + EOL +
        "USER memcache" + EOL +
        "HEALTHCHECK --interval=2s --start-period=4s --timeout=1s --retries=30 CMD /usr/bin/nc -z 127.0.0.1 11211"
    }
    new GenericContainer(
      // We also give it a tag and tell Docker to keep it around for future test
      // runs - it wouldn't make sense to rebuild it for all the tests in this
      // scheme!
      new ImageFromDockerfile("memcached:alpine-healthcheck", false)
        .withDockerfile(Path.of(tempDir + "/Dockerfile")),
      exposedPorts = Seq(11211),
      waitStrategy = Some(Wait.forHealthcheck())
    )
  }

  /** Once the container has started, we don't need the temp directory anymore.
    */
  override def afterStart(): Unit = {
    FileUtils.deleteDirectory(tempDir.toFile)
  }

  override def getCachingObject: CachingProxy = new CachingProxy() {
    useMemcached(
      ParameterTool.fromMap(Map(
        "caching.memcached.serverName" -> container.container.getHost,
        "caching.memcached.port" -> container.mappedPort(container.exposedPorts.head).toString
      ).asJava)
    )
  }

  "Memcached cache backend" should {
    behave like cachingBehaviours
  }
}
