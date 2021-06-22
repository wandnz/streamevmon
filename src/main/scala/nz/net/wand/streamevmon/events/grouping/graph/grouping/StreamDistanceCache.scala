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

package nz.net.wand.streamevmon.events.grouping.graph.grouping

import nz.net.wand.streamevmon.events.grouping.graph.impl.GraphType.GraphT
import nz.net.wand.streamevmon.measurements.traits.MeasurementMeta

import java.time.Instant

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.jgrapht.alg.util.UnorderedPair

import scala.collection.mutable
import scala.collection.JavaConverters._

/** Keeps track of the distances between measurement streams. Uses
  * [[DistanceBetweenStreams]] to calculate the distance. When an unrecognised
  * stream is added, relevant distances are recalculated. No calculations occur
  * if a recognised stream is added.
  */
class StreamDistanceCache extends CheckpointedFunction with Serializable {
  type MetaT = MeasurementMeta
  type PairT = UnorderedPair[MetaT, MetaT]

  val lastSeenTimes: mutable.Map[MetaT, Instant] = mutable.Map()
  val knownDistances: mutable.Map[PairT, StreamDistance] = mutable.Map()

  protected def addNewStream(graph: GraphT, meta: MetaT): Unit = {
    val knownStreams = lastSeenTimes.keys
    knownStreams
      .foreach { s =>
        knownDistances.put(new PairT(meta, s), DistanceBetweenStreams.get(graph, meta, s))
      }
  }

  def receiveStream(graph: GraphT, meta: MetaT, eventTime: Instant): Unit = {
    if (!lastSeenTimes.contains(meta)) {
      addNewStream(graph, meta)
    }
    lastSeenTimes.put(meta, eventTime)
  }

  def recalculateAllDistances(graph: GraphT): Unit = {
    knownDistances
      .foreach { case (pair, _) =>
        knownDistances.put(pair, DistanceBetweenStreams.get(graph, pair.getFirst, pair.getSecond))
      }
  }

  def clear(): Unit = {
    lastSeenTimes.clear()
    knownDistances.clear()
  }

  def getDistancesFor(meta: MetaT): Map[MetaT, StreamDistance] = {
    knownDistances
      .filter { case (metas, _) => metas.hasElement(meta) }
      .map { case (metas, value) =>
        (
          if (metas.getFirst == meta) {
            metas.getSecond
          }
          else {
            metas.getFirst
          },
          value
        )
      }
      .toMap
  }

  def getDistanceBetween(a: MetaT, b: MetaT): Option[StreamDistance] = {
    if (a == b) {
      Some(StreamDistance.ZERO)
    }
    else {
      knownDistances.get(new PairT(a, b))
    }
  }

  // == CheckpointedFunction implementation ==

  private var lastSeenState: ListState[(MetaT, Instant)] = _
  private var distancesState: ListState[(PairT, StreamDistance)] = _

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    lastSeenState.clear()
    lastSeenState.addAll(lastSeenTimes.toList.asJava)
    distancesState.clear()
    distancesState.addAll(knownDistances.toList.asJava)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    lastSeenState = context
      .getOperatorStateStore
      .getUnionListState(new ListStateDescriptor("lastSeen", classOf[(MetaT, Instant)]))
    distancesState = context
      .getOperatorStateStore
      .getUnionListState(new ListStateDescriptor("distances", classOf[(PairT, StreamDistance)]))

    if (context.isRestored) {
      lastSeenState.get.forEach(entry => lastSeenTimes.put(entry._1, entry._2))
      distancesState.get.forEach(entry => knownDistances.put(entry._1, entry._2))
    }
  }
}
