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

package nz.net.wand.streamevmon.flink

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.collection.JavaConverters._

/** This function works like the .zip() operator on two collections, but for
  * DataStreams. It has no configuration, but does utilise checkpoints to
  * maintain an internal buffer of yet-unpaired items.
  */
class ZipFunction[IN1: TypeInformation, IN2: TypeInformation]
  extends CoProcessFunction[IN1, IN2, (IN1, IN2)]
          with CheckpointedFunction
          with HasFlinkConfig {
  override val flinkName: String = "Zip Function"
  override val flinkUid: String = "zip"
  override val configKeyGroup: String = ""

  protected val buf1: mutable.Queue[IN1] = mutable.Queue()
  protected val buf2: mutable.Queue[IN2] = mutable.Queue()

  override def processElement1(
    value                           : IN1,
    ctx                             : CoProcessFunction[IN1, IN2, (IN1, IN2)]#Context,
    out                             : Collector[(IN1, IN2)]
  ): Unit = {
    if (buf2.nonEmpty) {
      out.collect((value, buf2.dequeue()))
    }
    else {
      buf1.enqueue(value)
    }
  }

  override def processElement2(
    value                           : IN2,
    ctx                             : CoProcessFunction[IN1, IN2, (IN1, IN2)]#Context,
    out                             : Collector[(IN1, IN2)]
  ): Unit = {
    if (buf1.nonEmpty) {
      out.collect((buf1.dequeue(), value))
    }
    else {
      buf2.enqueue(value)
    }
  }

  private var buf1state: ListState[IN1] = _
  private var buf2state: ListState[IN2] = _

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    buf1state.clear()
    buf1state.addAll(buf1.asJava)
    buf2state.clear()
    buf2state.addAll(buf2.asJava)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    buf1state = context
      .getOperatorStateStore
      .getListState(new ListStateDescriptor[IN1]("buf1", createTypeInformation[IN1]))
    buf2state = context
      .getOperatorStateStore
      .getListState(new ListStateDescriptor[IN2]("buf2", createTypeInformation[IN2]))

    if (context.isRestored) {
      buf1state.get().forEach { item => buf1.enqueue(item) }
      buf2state.get().forEach { item => buf2.enqueue(item) }
    }
  }
}
