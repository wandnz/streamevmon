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

package nz.net.wand.streamevmon.events.grouping.graph.pruning

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.JavaConverters._

/** A Map that allows defining multiple keys per value, but is still accessed
  * in the usual key-first manner. The underlying map contains only a single
  * instance of each value.
  *
  * If two keys are created with the same value, then any subsequent updates to
  * either key will also update the value associated with the other key.
  */
class MergeMap[K, V] extends CheckpointedFunction {

  type ValueOrPointer = Either[K, V]

  val theMap: mutable.Map[K, ValueOrPointer] = mutable.Map()

  /** Updates the value associated with a key. Also updates the values of any
    * other keys that had the same value.
    */
  def put(k: K, v: V): Unit = {
    // Get any other instances of v that already exist as values in the map
    val otherKeysWithValuesThatAreV = theMap.filter {
      case (_, Left(_)) => false
      // skip the one that matches the key we're updating
      case (theKey, Right(value)) => k != theKey && v == value
    }
    // If there's more than one, something's gone wrong
    if (otherKeysWithValuesThatAreV.size > 1) {
      throw new IllegalStateException(s"There should only be one Right(v) with a particular value in theMap! $k: $v")
    }
    // If there's exactly one, update k to point to it
    else if (otherKeysWithValuesThatAreV.nonEmpty) {
      theMap.put(k, Left(otherKeysWithValuesThatAreV.head._1))
    }
    // If it doesn't exist in the map yet, put it in as a new entry
    else {
      theMap.put(k, Right(v))
    }
  }

  /** Merges a number of keys, updating the associated value of all such keys
    * to the new value.
    *
    * @param ks      The keys that should be updated
    * @param resultK A new or existing key for the new value
    * @param resultV The new value
    */
  def merge(ks: Iterable[K], resultK: K, resultV: V): Unit = {
    // Place the new concrete value in
    this.put(resultK, resultV)
    // Then update all the other keys
    // If we don't do this filter, and ks.contains(resultK), we get k => Left(k),
    // which is an infinite loop and bad
    ks.filterNot(_ == resultK).foreach { k =>
      // If an entry we're updating is already pointing to something, the thing
      // it's pointing to needs to be updated too. Make it point at the new merged entry.
      this.getResultingKey(k).filterNot(_ == resultK).foreach { thatOne =>
        theMap.put(thatOne, Left(resultK))
      }
      theMap.put(k, Left(resultK))
    }
  }

  /** Gets the value associated with a key, or None if it is not found.
    */
  @tailrec
  final def get(k: K): Option[V] = {
    theMap.get(k) match {
      case Some(Right(value)) => Some(value)
      case Some(Left(value)) => get(value)
      case None => None
    }
  }

  /** Gets the key that is associated directly, rather than indirectly, with
    * the provided key's value.
    */
  @tailrec
  protected final def getResultingKey(k: K): Option[K] = {
    theMap.get(k) match {
      case Some(Right(_)) => Some(k)
      case Some(Left(value)) => getResultingKey(value)
      case None => None
    }
  }

  /** Calculates the number of lookups required to get the value associated
    * with each key, and presents the results as a map where the key is the
    * number of lookups, and the value is the number of keys that require that
    * many lookups.
    */
  def getPathDepths: Map[Int, Int] = {
    @tailrec
    def getDepthOfKey(k: K, depth: Int = 0): Int = {
      theMap.get(k) match {
        case Some(Right(_)) => depth
        case Some(Left(value)) => getDepthOfKey(value, depth + 1)
        case None => -1
      }
    }

    theMap.keys.map(getDepthOfKey(_)).groupBy(d => d).mapValues(_.size)
  }

  private var state: ListState[(K, ValueOrPointer)] = _

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    state.clear()
    state.addAll(theMap.toList.asJava)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    state = context
      .getOperatorStateStore
      .getUnionListState(new ListStateDescriptor("theMap", classOf[(K, ValueOrPointer)]))

    if (context.isRestored) {
      state.get.forEach(entry => theMap.put(entry._1, entry._2))
    }
  }
}
