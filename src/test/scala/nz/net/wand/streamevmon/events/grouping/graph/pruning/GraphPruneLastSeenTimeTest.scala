package nz.net.wand.streamevmon.events.grouping.graph.pruning

import nz.net.wand.streamevmon.TestBase
import nz.net.wand.streamevmon.events.grouping.graph.{GraphConstructionLogic, Host, NoReflectionUnusableEdgeSupplier}

import java.time.{Duration, Instant}

import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.AsUndirectedGraph

import scala.collection.JavaConverters._

class GraphPruneLastSeenTimeTest extends TestBase {

  val now = Instant.ofEpochMilli(1000000000000L)

  type VertexT = GraphConstructionLogic#VertexT
  type EdgeT = GraphConstructionLogic#EdgeT
  type GraphT = GraphConstructionLogic#GraphT

  def getHost(id: Int): VertexT = {
    Host(
      Set(),
      Set(),
      Set((id, 0, 0)),
      None
    )
  }

  def getEdge(age: Duration): EdgeT = {
    new EdgeT(now.minus(age))
  }

  def constructTestGraph: GraphT = {
    val graph = new GraphT(classOf[EdgeT])
    graph.setEdgeSupplier(new NoReflectionUnusableEdgeSupplier[EdgeT])

    Range(0, 25).foreach(id => graph.addVertex(getHost(id)))

    Range(0, 25).sliding(2).foreach { ids =>
      graph.addEdge(getHost(ids(0)), getHost(ids(1)), getEdge(Duration.ofHours(ids(1))))
    }

    graph
  }

  "GraphPruneLastSeenTime" should {
    "prune the correct edges" in {
      val pruner = new GraphPruneLastSeenTime[VertexT, EdgeT, GraphT](Duration.ofHours(12), now)

      val graph = constructTestGraph
      pruner.prune(graph)

      graph.edgeSet.asScala.foreach {
        _.lastSeen.compareTo(now.minus(Duration.ofHours(12))) should be >= 0
      }
    }

    "prune unconnected vertices" in {
      val pruner = new GraphPruneLastSeenTime[VertexT, EdgeT, GraphT](Duration.ofHours(12), now)

      val graph = constructTestGraph
      pruner.prune(graph)

      val allPaths = new DijkstraShortestPath(new AsUndirectedGraph(graph))
      graph.vertexSet.asScala.foreach { start =>
        graph.vertexSet.asScala.foreach { end =>
          allPaths.getPath(start, end) shouldNot be(null)
        }
      }
    }
  }
}
