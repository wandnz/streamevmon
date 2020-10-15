package nz.net.wand.streamevmon.connectors

import nz.net.wand.streamevmon.{PostgresContainerSpec, SeedData}

class AsInetPathTest extends PostgresContainerSpec {
  "AsInetPath" should {
    "be created correctly" in {
      val inetPath = SeedData.traceroute.expectedPath
      val asPath = SeedData.traceroute.expectedAsPath
      val meta = SeedData.traceroute.expectedMeta

      // TODO
      //AsInetPath(inetPath.path, Some(asPath.aspath), meta) shouldBe SeedData.traceroute.expectedAsInetPath
    }
  }
}
