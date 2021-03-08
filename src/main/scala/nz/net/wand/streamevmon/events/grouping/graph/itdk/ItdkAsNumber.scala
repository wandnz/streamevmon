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

import nz.net.wand.streamevmon.connectors.postgres.schema.AsNumber

import scala.util.Try

/** An AS number obtained from an ITDK dataset. We retain the node ID and method
  * because they're there in the file, but we don't really need either of them.
  */
class ItdkAsNumber(
  val nodeId     : Long,
  private val num: Int,
  val method     : Option[ItdkAsMethod.Value]
) extends AsNumber(num)

object ItdkAsNumber {
  def apply(line: String): ItdkAsNumber = {
    val parts = line.split(" ")
    new ItdkAsNumber(
      parts(1).drop(1).toInt,
      parts(2).toInt,
      if (parts.size > 3) {
        Try(ItdkAsMethod.withName(parts(3))).toOption
      }
      else {
        None
      }
    )
  }
}

object ItdkAsMethod extends Enumeration {
  val Interfaces: Value = Value("interfaces")
  val Refinement: Value = Value("refinement")
  val LastHop: Value = Value("last_hop")
}
