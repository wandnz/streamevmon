package nz.net.wand.streamevmon.events.grouping.graph.pruning

import nz.net.wand.streamevmon.TestBase
import nz.net.wand.streamevmon.events.grouping.graph._

import java.time.Instant

import scala.collection.mutable
import scala.collection.JavaConverters._

class GraphPruneParallelAnonymousHostTest extends TestBase with GraphConstructionLogic {

  override val getMergedHosts = mutable.Map[String, VertexT]()

  val topVertex: VertexT = Host(
    Set("Top"),
    Set(),
    Set(),
    None
  )

  val bottomVertex: VertexT = Host(
    Set("Bottom"),
    Set(),
    Set(),
    None
  )

  def getHost(id: Int): VertexT = {
    Host(
      Set(),
      Set(),
      Set((id, 0, 0)),
      None
    )
  }

  def getMergedHosts(ids: Int*): VertexT = {
    Host(
      Set(),
      Set(),
      ids.map(id => (id, 0, 0)).toSet,
      None
    )
  }

  def getEdge: EdgeT = new EdgeT(Instant.now())

  def constructTestGraph: GraphT = {
    val graph = new GraphT(classOf[EdgeT])
    graph.setEdgeSupplier(new NoReflectionUnusableEdgeSupplier[EdgeT])

    graph.addVertex(topVertex)
    Range(0, 20).foreach(id => graph.addVertex(getHost(id)))
    graph.addVertex(bottomVertex)

    Range(0, 20).foreach { id =>
      id % 4 match {
        case 0 => graph.addEdge(topVertex, getHost(id), getEdge)
        case 1 | 2 => graph.addEdge(getHost(id - 1), getHost(id), getEdge)
        case 3 =>
          graph.addEdge(getHost(id - 1), getHost(id), getEdge)
          graph.addEdge(getHost(id), bottomVertex, getEdge)
      }
    }

    graph
  }

  def constructExpectedGraph: GraphT = {
    val graph = new GraphT(classOf[EdgeT])
    graph.setEdgeSupplier(new NoReflectionUnusableEdgeSupplier[EdgeT])

    graph.addVertex(topVertex)
    val mergedHosts = Range(0, 4).map { id =>
      val host = getMergedHosts(id, id + 4, id + 8, id + 12, id + 16)
      graph.addVertex(host)
      host
    }
    graph.addVertex(bottomVertex)

    graph.addEdge(topVertex, mergedHosts.head, getEdge)
    mergedHosts.sliding(2).foreach { hosts =>
      graph.addEdge(hosts.head, hosts.drop(1).head, getEdge)
    }
    graph.addEdge(mergedHosts.last, bottomVertex, getEdge)

    graph
  }

  "GraphPruneParallelAnonymousHost" should {
    "merge hosts correctly" in {
      val graph = constructTestGraph
      val expected = constructExpectedGraph

      val pruner = new GraphPruneParallelAnonymousHost[VertexT, EdgeT, GraphT](
        getMergedHosts,
        (oldH, newH) => addOrUpdateVertex(graph, oldH, newH)
      )

      pruner.prune(graph)

      def comparableEdges(g: GraphT) = {
        g.edgeSet.asScala.map { e =>
          (g.getEdgeSource(e), g.getEdgeTarget(e))
        }
      }

      graph.vertexSet shouldBe expected.vertexSet
      comparableEdges(graph) shouldBe comparableEdges(expected)
    }
  }
}
