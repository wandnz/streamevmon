package nz.net.wand.streamevmon.events

import nz.net.wand.streamevmon.connectors.postgres.AsNumber

import java.awt.Color
import java.io.File

import org.jgrapht.nio.dot.DOTExporter
import org.jgrapht.nio.DefaultAttribute

import scala.collection.JavaConverters._

/** Exports a graph to a .dot file, with the vertexes coloured according to what
  * AS they belong to.
  */
object AmpletGraphDotExporter {
  def exportGraph(
    graph: AmpletGraphBuilder#GraphT,
    file : File
  ): Unit = {
    val exporter = new DOTExporter[AmpletGraphBuilder#VertexT, AmpletGraphBuilder#EdgeT]

    // We want to make sure that nodes have their addresses printed as their
    // names.
    exporter.setVertexIdProvider(entry => s""" "${entry.toString}" """.trim)

    // We also want to give them a colour defined by AS.
    val asNumberIndex: Map[Int, Int] = graph
      .vertexSet
      .asScala
      .toList
      .flatMap(_.as.number)
      .toSet
      .zipWithIndex
      .toMap

    // These colours are evenly distributed around hue-space, so they're all
    // reasonably different and black labels should be legible against them.
    def getAsColor(asn: AsNumber): String = asn.number match {
      case None => "#FFFFFF"
      case Some(num) =>
        val hue = (1.0 * (asNumberIndex(num).toDouble / asNumberIndex.size.toDouble)) % 1
        val color = Color.getHSBColor(hue.toFloat, 0.5f, 0.95f)
        f"#${color.getRed}%02X${color.getGreen}%02X${color.getBlue}%02X"
    }

    exporter.setVertexAttributeProvider { entry =>
      Map(
        "style" -> DefaultAttribute.createAttribute("filled"),
        "fillcolor" -> DefaultAttribute.createAttribute(s"${getAsColor(entry.as)}")
      ).asJava
    }

    // A dot file is just a listing of vertices and edges, and contains nothing
    // about layout. That's left to the renderer, which isn't our problem.
    exporter.exportGraph(graph, file)
  }
}
