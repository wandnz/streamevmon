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

package nz.net.wand.streamevmon.runners.unified.schema

import org.apache.flink.api.java.utils.ParameterTool

/** Enum proxy for types of Flink Window, including optional config overrides.
  * This lets windowed detectors specify the options they want for their windows
  * when they get built, based on their config.
  */
object StreamWindowType extends Enumeration {
  val Time: TimeWithOverrides = TimeWithOverrides(None)
  val Count: CountWithOverrides = CountWithOverrides(None, None)

  case class TimeWithOverrides(
    size: Option[ParameterTool => Long]
  ) extends Val("time")

  case class CountWithOverrides(
    size: Option[ParameterTool => Long],
    slide: Option[ParameterTool => Long]
  ) extends Val("count")

}
