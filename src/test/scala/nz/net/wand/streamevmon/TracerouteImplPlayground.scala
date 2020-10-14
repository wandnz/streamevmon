package nz.net.wand.streamevmon

import nz.net.wand.streamevmon.events.{AmpletGraphBuilder, AmpletGraphDotExporter}

import java.io.File

import scala.concurrent.duration.DurationInt

class TracerouteImplPlayground extends PostgresContainerSpec {
  "Traceroute" should {
    "work from class" in {
      val builder = new AmpletGraphBuilder(getPostgres, ttl = Some(10.seconds))
      builder.rebuildGraph()
      AmpletGraphDotExporter.exportGraph(builder.graph.get, new File("out/traceroute.dot"))
      builder.invalidateCaches()
    }
  }
}
