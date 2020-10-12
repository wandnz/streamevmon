package nz.net.wand.streamevmon.connectors

import nz.net.wand.streamevmon.{PostgresContainerSpec, SeedData}
import nz.net.wand.streamevmon.connectors.postgres.AsInetPath

class AsInetPathTest extends PostgresContainerSpec {
  "AsInetPath" should {
    "be created correctly" in {
      val inetPath = SeedData.traceroute.expectedPath
      val asPath = SeedData.traceroute.expectedAsPath

      AsInetPath(inetPath.path, Some(asPath.aspath)) shouldBe SeedData.traceroute.expectedAsInetPath
    }
  }
}
