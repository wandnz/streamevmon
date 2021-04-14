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

package nz.net.wand.streamevmon.events.grouping.graph

import java.time.Instant
import java.util.Objects

/** A simple edge type that holds an Instant, used to keep track of when the
  * edge was last seen to allow edge expiry.
  */
class EdgeWithLastSeen(val lastSeen: Instant, val uid: String) {
  def this(
    lastSeen: Instant,
    source  : Object,
    destination: Object
  ) = {
    this(
      lastSeen,
      (source.hashCode + destination.hashCode).toHexString
    )
  }

  override def hashCode(): Int = Objects.hash(lastSeen, uid)

  override def equals(obj: Any): Boolean = {
    obj match {
      case e: EdgeWithLastSeen => e.lastSeen == lastSeen && e.uid == uid
      case _ => false
    }
  }
}

