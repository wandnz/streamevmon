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

package nz.net.wand.streamevmon.detectors.changepoint

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.traits.{HasDefault, Measurement}
import nz.net.wand.streamevmon.parameters.{HasParameterSpecs, ParameterSpec}
import nz.net.wand.streamevmon.parameters.constraints.ParameterConstraint

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

/** Wrapper class for [[ChangepointProcessor]].
  * This part does Flink stuff, and stores the ChangepointProcessor in a way that
  * Flink should be able to serialise to restore saved state.
  *
  * @param initialDistribution The distribution that should be used as a base
  *                            when adding new measurements to the runs.
  * @param shouldDoGraphs      When true, .csv files are output into the ./out/graphs
  *                            directory which can be used to produce graphs of the
  *                            state of the detector. Defaults to false.
  * @param filename            When shouldDoGraphs is true, this filename is combined with
  *                            the values of the parameters set in the global configuration
  *                            to create the filename of the .csv files output.
  * @tparam MeasT The type of Measurement we're receiving.
  * @tparam DistT The type of Distribution to model recent measurements with.
  */
class ChangepointDetector[
  MeasT <: Measurement with HasDefault : TypeInformation,
  DistT <: Distribution[MeasT] : TypeInformation
](
  initialDistribution: DistT,
  shouldDoGraphs     : Boolean = false,
  filename           : Option[String] = None
) extends KeyedProcessFunction[String, MeasT, Event]
          with HasFlinkConfig
          with Logging {

  final val flinkName = s"Changepoint Detector (${initialDistribution.distributionName})"
  final val flinkUid = s"changepoint-detector-${initialDistribution.distributionName}"
  final val configKeyGroup = "changepoint"

  private var processor: ValueState[ChangepointProcessor[MeasT, DistT]] = _

  override def open(parameters: Configuration): Unit = {
    processor = getRuntimeContext.getState(
      new ValueStateDescriptor[ChangepointProcessor[MeasT, DistT]](
        "Changepoint Processor",
        TypeInformation.of(classOf[ChangepointProcessor[MeasT, DistT]])
      )
    )
  }

  override def processElement(
    value: MeasT,
    ctx  : KeyedProcessFunction[String, MeasT, Event]#Context,
    out  : Collector[Event]
  ): Unit = {
    if (processor.value == null) {
      processor.update(new ChangepointProcessor[MeasT, DistT](initialDistribution, configKeyGroup, shouldDoGraphs, filename))
      processor.value.open(configWithOverride(getRuntimeContext))
    }
    processor.value.processElement(value, out)
  }
}

object ChangepointDetector extends HasParameterSpecs {

  private val maxHistorySpec = ParameterSpec(
    "detector.changepoint.maxHistory",
    60,
    Some(1),
    Some(600)
  )
  private val triggerCountSpec = ParameterSpec(
    "detector.changepoint.triggerCount",
    40,
    Some(1),
    Some(600)
  )
  private val ignoreOutlierNormalCountSpec = ParameterSpec(
    "detector.changepoint.ignoreOutlierNormalCount",
    1,
    Some(0),
    Some(600)
  )
  private val inactivityPurgeTimeSpec = ParameterSpec(
    "detector.changepoint.inactivityPurgeTime",
    60,
    Some(0),
    Some(Int.MaxValue),
  )
  private val minimumEventIntervalSpec = ParameterSpec(
    "detector.changepoint.minimumEventInterval",
    10,
    Some(0),
    Some(600)
  )
  private val severityThresholdSpec = ParameterSpec(
    "detector.changepoint.severityThreshold",
    30,
    Some(0),
    Some(100)
  )

  override val parameterSpecs: Seq[ParameterSpec[Any]] = Seq(
    maxHistorySpec,
    triggerCountSpec,
    ignoreOutlierNormalCountSpec,
    inactivityPurgeTimeSpec,
    minimumEventIntervalSpec,
    severityThresholdSpec
  ).asInstanceOf[Seq[ParameterSpec[Any]]]

  override val parameterRestrictions: Seq[ParameterConstraint.ComparableConstraint[Any]] = Seq(
    ParameterConstraint.LessThan(
      triggerCountSpec,
      maxHistorySpec
    ),
    ParameterConstraint.LessThan(
      minimumEventIntervalSpec,
      inactivityPurgeTimeSpec
    )
  ).asInstanceOf[Seq[ParameterConstraint.ComparableConstraint[Any]]]
}
