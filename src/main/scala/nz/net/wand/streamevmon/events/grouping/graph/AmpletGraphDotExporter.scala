package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.connectors.postgres.{AsNumber, AsNumberCategory}

import java.awt.Color
import java.io.File

import org.jgrapht.graph.{DefaultDirectedWeightedGraph, DefaultWeightedEdge}
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.dot.DOTExporter

import scala.collection.JavaConverters._

/** Exports a graph to a .dot file, with the vertexes coloured according to what
  * AS they belong to.
  */
object AmpletGraphDotExporter {
  def exportGraph[VertexT <: Host](
    graph: DefaultDirectedWeightedGraph[VertexT, DefaultWeightedEdge],
    file : File
  ): Unit = {
    val exporter = new DOTExporter[VertexT, DefaultWeightedEdge]

    // We want to make sure that nodes have their addresses printed as their
    // names.
    exporter.setVertexIdProvider(entry => s""" "${entry.toString}" """.trim)

    // We also want to give them a colour defined by AS.
    val asNumberIndex: Map[Int, Int] = graph.vertexSet.asScala.toList
      .flatMap {
        case host: HostWithKnownHostname => host.addresses.map(_._2)
        case host: HostWithUnknownHostname => Some(host.address._2)
        case _: HostWithUnknownAddress => None
      }
      .flatMap(_.number)
      .toSet
      .zipWithIndex
      .toMap

    // These colours are evenly distributed around hue-space, so they're all
    // reasonably different and black labels should be legible against them.
    def getAsColor(asn: AsNumber): String = {
      asn.number match {
        case None => "#FFFFFF"
        case Some(num) =>
          val hue = (0.8 * (asNumberIndex(num).toDouble / asNumberIndex.size.toDouble) + 0.1) % 1
          val color = Color.getHSBColor(hue.toFloat, 0.5f, 0.95f)
          f"#${color.getRed}%02X${color.getGreen}%02X${color.getBlue}%02X"
      }
    }

    val namedColor = "#FF0000"

    exporter.setVertexAttributeProvider { entry =>
      val isProbablyAmplet = entry match {
        case host: HostWithKnownHostname => host.hostname.contains("amp")
        case _ => false
      }
      Map(
        "style" -> DefaultAttribute.createAttribute("filled"),
        "shape" -> DefaultAttribute.createAttribute(if (isProbablyAmplet) {
          "box"
        }
        else {
          "oval"
        }),
        "fillcolor" -> DefaultAttribute.createAttribute(
          entry match {
            case _: HostWithKnownHostname => namedColor
            case host: HostWithUnknownHostname => getAsColor(host.address._2)
            case _: HostWithUnknownAddress => getAsColor(AsNumber(AsNumberCategory.Missing.id))
          }
        )
      ).asJava
    }

    // A dot file is just a listing of vertices and edges, and contains nothing
    // about layout. That's left to the renderer, which isn't our problem.
    exporter.exportGraph(graph, file)
  }
}
