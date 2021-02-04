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

/** This is part of an [[AsPath]]. `hopsInAs` is the number of
  * separate addresses which were visited before leaving the AS described
  * in `asNumber`.
  */
case class AsPathEntry(hopsInAs: Int, asNumber: AsNumber)

object AsPathEntry {
  /** Interprets the format from AMP's PostgreSQL database. */
  def apply(entry: String): AsPathEntry = {
    val parts = entry.replace("\"", "").split('.')
    try {
      new AsPathEntry(parts(0).toInt, AsNumber(parts(1).toInt))
    }
    catch {
      case e: NumberFormatException => throw e
    }
  }
}
