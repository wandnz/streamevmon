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

package nz.net.wand.streamevmon.connectors.postgres.schema

import nz.net.wand.streamevmon.events.grouping.graph.impl.SerializableInetAddress

/** One entry in an [[AsInetPath]]. `address` is optional, as the address of a
  * hop may be unknown. AsNumber can be part of any of its categories, including
  * Unknown or Missing.
  */
case class AsInetPathEntry(
  address: Option[SerializableInetAddress],
  as     : AsNumber
) {

  override def toString: String = {
    address match {
      case Some(value) => value.toString + s" (${as.toString})"
      case None => s"?.?.?.?"
    }
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[AsInetPathEntry]

  override def equals(other: Any): Boolean = other match {
    case that: AsInetPathEntry =>
      (that canEqual this) &&
        address == that.address &&
        as == that.as
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(address, as)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
