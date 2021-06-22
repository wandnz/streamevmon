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

import org.apache.flink.api.java.utils.ParameterTool

/** Convenience class to store configurations for a frequency level.
  * used exclusively in `[[FrequentEventFilter]]`.
  */
case class FrequencyConfig(
  name    : String,
  interval: Int,
  count   : Int,
  cooldown: Int,
  severity: Int
)

object FrequencyConfig {
  val configKeyNames = Seq("interval", "count", "cooldown", "severity")

  def apply(name: String, configKeyGroup: String, getDefaults: Boolean, conf: ParameterTool): FrequencyConfig = if (getDefaults) {
    FrequencyConfig(
      name,
      conf.getInt(s"eventGrouping.$configKeyGroup.defaults.$name.interval"),
      conf.getInt(s"eventGrouping.$configKeyGroup.defaults.$name.count"),
      conf.getInt(s"eventGrouping.$configKeyGroup.defaults.$name.cooldown"),
      conf.getInt(s"eventGrouping.$configKeyGroup.defaults.$name.severity"),
    )
  }
  else {
    FrequencyConfig(
      name,
      conf.getInt(s"eventGrouping.$configKeyGroup.$name.interval"),
      conf.getInt(s"eventGrouping.$configKeyGroup.$name.count"),
      conf.getInt(s"eventGrouping.$configKeyGroup.$name.cooldown"),
      conf.getInt(s"eventGrouping.$configKeyGroup.$name.severity"),
    )
  }
}
