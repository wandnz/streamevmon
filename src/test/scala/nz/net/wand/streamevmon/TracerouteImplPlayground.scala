package nz.net.wand.streamevmon

import nz.net.wand.streamevmon.events.{AmpletGraphBuilder, AmpletGraphDotExporter}

import java.io.{File, FileInputStream, ObjectInputStream}

import scala.collection.JavaConverters._
import scala.concurrent.duration.DurationInt

class TracerouteImplPlayground extends PostgresContainerSpec {
  "Traceroute" should {
    "work from class" in {
      val builder = new AmpletGraphBuilder(getPostgres, ttl = Some(10.seconds))
      val graph = builder.rebuildGraph(
        pruneMissingInetAddresses = false,
        distinguishMissingInetAddresses = true,
        compressMissingInetChains = true
      )
      AmpletGraphDotExporter.exportGraph(graph, new File("out/traceroute.dot"))
    }

    "work from spkl" in {
      // TODO: This is quite impractical, it just takes too long to read the data
      //  from disk and rebuild the graph at the size that the cauldron database
      //  is. We should probably filter out the cauldron database such that fewer
      //  streams are involved. We should pick a small number of amplets and get
      //  all the data for them, instead of all the data for all the amplets.
      //  This should cut down on our runtimes considerably. We should also look
      //  over the interface for AmpletGraphBuilder again and see if it makes
      //  sense now that we've removed the internal graph storage. Perhaps it
      //  should be a collection of object methods that take a PostgresConnection
      //  as a parameter?

      println(1)
      val ois = new ObjectInputStream(new FileInputStream("out/traceroute_cauldron.spkl"))
      println(2)
      val graph = ois.readObject().asInstanceOf[AmpletGraphBuilder#GraphT]
      println(3)

      graph.vertexSet.asScala.filter(_.ampletHostname.isDefined).toSeq.sortBy(_.as.number).foreach(println)

      //val ois2 = new ObjectInputStream(new FileInputStream("out/traceroute_paths_cauldron.spkl"))
      //println(4)
      //val paths = ois2.readObject().asInstanceOf[Iterable[AsInetPath]]
      //println(5)
      //val graph2 = new AmpletGraphBuilder(getPostgres).buildGraph(paths, true, true)
      //println(6)
      //graph shouldBe graph2
    }
  }
}
