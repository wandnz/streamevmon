package nz.net.wand.streamevmon.connectors

import nz.net.wand.streamevmon.{PostgresContainerSpec, SeedData}
import nz.net.wand.streamevmon.connectors.postgres.schema.AsInetPath

class AsInetPathTest extends PostgresContainerSpec {
  "AsInetPath" should {
    "be created correctly" in {
      val inetPath = SeedData.traceroute.expectedPath
      val asPath = SeedData.traceroute.expectedAsPath
      val measurement = SeedData.traceroute.expected
      val meta = SeedData.traceroute.expectedMeta

      AsInetPath(inetPath.path, Some(asPath.aspath), measurement, meta) shouldBe SeedData.traceroute.expectedAsInetPath
      AsInetPath(inetPath.path, None, measurement, meta) shouldBe SeedData.traceroute.expectedAsInetPathWithoutAsPath
    }
  }
}
