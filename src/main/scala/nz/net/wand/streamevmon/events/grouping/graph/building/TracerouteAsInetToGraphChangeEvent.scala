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

package nz.net.wand.streamevmon.events.grouping.graph.building

import nz.net.wand.streamevmon.connectors.postgres.schema.AsInetPath
import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.events.grouping.graph.GraphType._
import nz.net.wand.streamevmon.events.grouping.graph.building.GraphChangeEvent._

import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector

/** Converts AsInetPaths to GraphChangeEvents in the most simple manner. This
  * class performs no alias resolution, pruning, or anything fancy, it's simply
  * a direct mapping of an AsInetPath to AddVertex and AddEdge descriptors.
  */
class TracerouteAsInetToGraphChangeEvent
  extends ProcessFunction[AsInetPath, GraphChangeEvent]
          with HasFlinkConfig
          with Logging {
  override val flinkName: String = "Traceroute AsInet => GraphChangeEvent"
  override val flinkUid: String = "traceroute-as-inet-to-graph-change-event"
  override val configKeyGroup: String = ""

  /** Converts an AsInetPath into a path of Hosts. */
  def pathToHosts(path: AsInetPath): Iterable[VertexT] = {
    val lastIndex = path.size - 1
    path.zipWithIndex.map { case (entry, index) =>
      // We can usually extract a hostname for the source and destination of
      // the test from the metadata.
      val hostname = index match {
        case 0 => Some(path.meta.source)
        case i if i == lastIndex => Some(path.meta.destination)
        case _ => None
      }

      val hostnames = hostname.toSet
      val addresses = entry.address.map(addr => (addr, entry.as)).toSet
      new VertexT(
        hostnames,
        addresses,
        if (hostnames.isEmpty && addresses.isEmpty) {
          Set((path.meta.stream, path.measurement.path_id, index))
        }
        else {
          Set()
        },
        None
      )
    }
  }

  override def processElement(
    value                          : AsInetPath,
    ctx                            : ProcessFunction[AsInetPath, GraphChangeEvent]#Context,
    out                            : Collector[GraphChangeEvent]
  ): Unit = {
    val hosts = pathToHosts(value)

    hosts.foreach(host => out.collect(AddVertex(host)))

    hosts
      .sliding(2)
      .foreach { elems =>
        val source = elems.head
        val dest = elems.drop(1).headOption
        dest.foreach { dst =>
          AddEdge(
            source,
            dst,
            new EdgeT(value.measurement.time, source, dst)
          )
        }
      }

    out.collect(MeasurementEndMarker(value.measurement.time))
  }
}
