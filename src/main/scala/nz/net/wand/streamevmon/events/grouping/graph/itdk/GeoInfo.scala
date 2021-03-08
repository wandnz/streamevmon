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

package nz.net.wand.streamevmon.events.grouping.graph.itdk

/** The geo information for one node in an ITDK dataset. */
case class GeoInfo(
  nodeId: Long,
  continent: String,
  country  : String,
  region   : String,
  city     : String,
  latitude : Float,
  longitude: Float
)

object GeoInfo {
  /** Transforms a line from the `nodes.geo` file in the dataset. */
  def apply(line: String): GeoInfo = {
    val parts = line.split("\t")
    GeoInfo(
      parts(0).drop(10).dropRight(1).toInt,
      parts(1),
      parts(2),
      parts(3),
      parts(4),
      parts(5).toFloat,
      parts(6).toFloat
    )
  }
}
