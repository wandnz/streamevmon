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

package nz.net.wand.streamevmon.events

import nz.net.wand.streamevmon.flink.HasFlinkConfig

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector

import scala.collection.JavaConverters._

/** Filters out events that occur frequently, replacing them with batched
  * summaries at certain intervals.
  *
  * This function should be placed directly after an event-producing operator,
  * such as a detector.
  *
  * ==Configuration==
  *
  * This class is configured by the `eventGrouping.frequentFilter` config
  * key group. The key group should contain a number of other key groups, each
  * of which should have fields as shown in the default settings below.
  *
  * {{{
  * eventGrouping:
  *   frequentFilter:
  *     small:
  *       interval: 60
  *       count: 5
  *     medium:
  *       interval: 120
  *       count: 15
  *     large:
  *       interval: 360
  *       count: 100
  * }}}
  *
  * These fields together determine the frequency of events that will trigger
  * filtering by that group. There must be `count` many events within `interval`
  * seconds.
  */
class FrequentEventFilter[T <: Event]
  extends ProcessFunction[T, T]
          with CheckpointedFunction
          with HasFlinkConfig {
  override val flinkName: String = "Frequent Event Filter"
  override val flinkUid: String = "frequent-event-filter"
  override val configKeyGroup: String = "frequentFilter"

  /** Convenience class to store configurations for a frequency level. */
  case class FrequencyConfig(
    name    : String,
    interval: Int,
    count   : Int
  )

  object FrequencyConfig {
    val configKeyNames = Seq("interval", "count")

    def apply(name: String, conf: ParameterTool): FrequencyConfig = FrequencyConfig(
      name,
      conf.getInt(s"eventGrouping.$configKeyGroup.$name.interval"),
      conf.getInt(s"eventGrouping.$configKeyGroup.$name.count")
    )
  }

  var configs: Iterable[FrequencyConfig] = _

  /** Parses the configuration section corresponding to this operator. Will
    * throw an IllegalArgumentException (or possibly other types, depending on
    * the issue) if anything is wrong with the format of the configuration.
    */
  def parseConfig(): Iterable[FrequencyConfig] = {
    val conf = configWithOverride(getRuntimeContext)
    conf
      .toMap
      .asScala
      .keySet
      // Grab the config keys for just this operator
      .filter(_.startsWith(s"eventGrouping.$configKeyGroup"))
      // Trim off the common section
      .map(_.drop(s"eventGrouping.$configKeyGroup.".length))
      .map { i =>
        // The rest of the config key should just be two parts...
        val parts = i.split('.')
        if (parts.length != 2) {
          throw new IllegalArgumentException(s"Config key eventGrouping.$configKeyGroup.$i unrecognised!")
        }
        else {
          // ... the name of the group, and one of the fields of a FrequencyConfig
          (parts.head, parts.drop(1).head)
        }
      }
      // Turn the list of tuples (name, key) into a Map[name -> Set[keys]]
      .groupBy(_._1)
      .mapValues(_.map(_._2))
      .map { case (groupName, configKeys) =>
        // If the group doesn't contain all the keys we expect, fail loudly
        // since it's invalid. We allow unrecognised keys to be present.
        if (FrequencyConfig.configKeyNames.exists(k => !configKeys.contains(k))) {
          throw new IllegalArgumentException(
            s"Config keys ${configKeys.mkString(",")} for group $groupName " +
              s"unrecognised! Expected ${FrequencyConfig.configKeyNames.mkString(",")}"
          )
        }
        else {
          // Now we know all the keys are there, we can go ahead and construct
          // a config for this group.
          FrequencyConfig(groupName, conf)
        }
      }
  }

  override def open(parameters: Configuration): Unit = {
    configs = parseConfig()
  }

  override def processElement(
    value                          : T,
    ctx                            : ProcessFunction[T, T]#Context,
    out                            : Collector[T]
  ): Unit = {
    out.collect(value)
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {

  }

  override def initializeState(context: FunctionInitializationContext): Unit = {

  }
}
