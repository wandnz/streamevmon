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
import nz.net.wand.streamevmon.measurements.amp.{Traceroute, TracerouteMeta}
import nz.net.wand.streamevmon.measurements.MeasurementMetaExtractor

import org.apache.flink.streaming.api.scala._

/** Contains a couple of functions that can insert graph-building steps to a
  * Flink pipeline.
  */
object FlinkHelpers {
  def tracerouteToAsInetPath(traceroutes: DataStream[Traceroute]): DataStream[AsInetPath] = {
    val tracerouteMetas = {
      val metaExtractor = new MeasurementMetaExtractor[Traceroute, TracerouteMeta]
      (
        traceroutes
          .process(metaExtractor)
          .name(metaExtractor.flinkName)
          .uid(metaExtractor.flinkUid),
        metaExtractor.outputTag
      )
    }

    val asInetPaths = {
      val extractor = new TracerouteAsInetPathExtractor
      traceroutes
        .connect(tracerouteMetas._1.getSideOutput(tracerouteMetas._2))
        .process(extractor)
        .name(extractor.flinkName)
        .uid(extractor.flinkUid)
    }

    asInetPaths
  }

  /** From a stream of AsInetPaths, generates a stream of GraphChangeEvents
    * with pruning and alias resolution included.
    */
  def asInetPathsToGraph(asInetPaths: DataStream[AsInetPath]): DataStream[GraphChangeEvent] = {
    val graphChangeEvents = {
      val function = new TracerouteAsInetToGraphChangeEvent()
      asInetPaths
        .process(function)
        .name(function.flinkName)
        .uid(function.flinkUid)
    }

    val lastSeenPruned = {
      val function = new GraphPruneLastSeenTimeEventGenerator()
      graphChangeEvents
        .process(function)
        .name(function.flinkName)
        .uid(function.flinkUid)
    }

    // This function can add extra aliases that we need the
    // GraphChangeAliasResolution to know about, since we want to keep all the
    // alias resolution happening in one place. This means it has to happen
    // upstream from that function, which does unfortunately mean that the graph
    // it holds will be a bit bigger than the rest of them due to lack of alias
    // resolution.
    val noParallelAnonymousHosts = {
      val function = new GraphPruneParallelAnonymousHostEventGenerator()
      lastSeenPruned
        .process(function)
        .name(function.flinkName)
        .uid(function.flinkUid)
    }

    val aliasesResolved = {
      val function = new GraphChangeAliasResolution()
      noParallelAnonymousHosts
        .process(function)
        .name(function.flinkName)
        .uid(function.flinkUid)
    }

    aliasesResolved
  }

  /** Given a stream of traceroute data, outputs a stream of GraphChangeEvents
    * with pruning and alias resolution built in.
    */
  def tracerouteToGraph(traceroutes: DataStream[Traceroute]): DataStream[GraphChangeEvent] = {
    asInetPathsToGraph(tracerouteToAsInetPath(traceroutes))
  }
}
