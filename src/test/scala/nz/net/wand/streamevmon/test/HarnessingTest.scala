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

package nz.net.wand.streamevmon.test

import nz.net.wand.streamevmon.measurements.MeasurementKeySelector
import nz.net.wand.streamevmon.measurements.traits.Measurement
import nz.net.wand.streamevmon.Configuration

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.functions.KeySelector
import org.apache.flink.streaming.api.functions.{KeyedProcessFunction, ProcessFunction}
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.streaming.api.operators.{KeyedProcessOperator, ProcessOperator}
import org.apache.flink.streaming.api.operators.co.CoProcessOperator
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.util._

import scala.reflect.ClassTag

trait HarnessingTest extends TestBase {

  // Defaults to the nearest round number above Y2K, to give us some leeway
  // after the epoch for detectors which have a minimum event interval.
  protected var currentTime: Long = 1000000000000L

  protected var checkpointId: Long = 1L

  protected def newHarness[I <: Measurement : ClassTag, O](
    function: ProcessFunction[I, O]
  ): OneInputStreamOperatorTestHarness[I, O] = {
    val h = new OneInputStreamOperatorTestHarness(
      new ProcessOperator(function)
    )
    h.getExecutionConfig.setGlobalJobParameters(Configuration.get())
    h
  }

  protected def newHarness[K: TypeInformation, I, O](
    function   : KeyedProcessFunction[K, I, O],
    keySelector: KeySelector[I, K]
  ): KeyedOneInputStreamOperatorTestHarness[K, I, O] = {
    val h = new KeyedOneInputStreamOperatorTestHarness(
      new KeyedProcessOperator(function),
      keySelector,
      createTypeInformation[K]
    )
    h.getExecutionConfig.setGlobalJobParameters(Configuration.get())
    h
  }

  protected def newHarness[I <: Measurement : ClassTag, TypeInformation, O](
    function: KeyedProcessFunction[String, I, O]
  ): KeyedOneInputStreamOperatorTestHarness[String, I, O] = {
    newHarness(function, new MeasurementKeySelector[I]())
  }

  protected def newHarness[IN1, IN2, OUT](
    function: CoProcessFunction[IN1, IN2, OUT]
  ): TwoInputStreamOperatorTestHarness[IN1, IN2, OUT] = {
    val h = new TwoInputStreamOperatorTestHarness(
      new CoProcessOperator(function)
    )
    h.getExecutionConfig.setGlobalJobParameters(Configuration.get())
    h
  }

  protected def snapshotAndRestart[I <: Measurement : ClassTag, O](
    harness : OneInputStreamOperatorTestHarness[I, O],
    function: ProcessFunction[I, O]
  ): OneInputStreamOperatorTestHarness[I, O] = {
    snapshotAndRestart[O, OneInputStreamOperatorTestHarness[I, O]](harness, newHarness(function))
  }

  protected def snapshotAndRestart[I <: Measurement : ClassTag, O](
    harness : KeyedOneInputStreamOperatorTestHarness[String, I, O],
    function: KeyedProcessFunction[String, I, O]
  ): KeyedOneInputStreamOperatorTestHarness[String, I, O] = {
    snapshotAndRestart[O, KeyedOneInputStreamOperatorTestHarness[String, I, O]](harness, newHarness(function))
  }

  protected def snapshotAndRestart[IN1, IN2, OUT](
    harness : TwoInputStreamOperatorTestHarness[IN1, IN2, OUT],
    function: CoProcessFunction[IN1, IN2, OUT]
  ): TwoInputStreamOperatorTestHarness[IN1, IN2, OUT] = {
    snapshotAndRestart[OUT, TwoInputStreamOperatorTestHarness[IN1, IN2, OUT]](harness, newHarness(function))
  }

  protected def snapshotAndRestart[O, HarnessT <: AbstractStreamOperatorTestHarness[O]](
    harness: HarnessT,
    newHarness: HarnessT
  ) = {
    currentTime += 1
    checkpointId += 1
    val snapshot = harness.snapshot(checkpointId, currentTime)
    harness.close()

    newHarness.setup()
    newHarness.initializeState(snapshot)
    newHarness.open()
    newHarness
  }
}
