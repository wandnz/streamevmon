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

package nz.net.wand.streamevmon.measurements.traits

import java.time.Instant

/** Represents a single measurement at a point in time.
  */
trait Measurement extends Serializable {

  /** Measurements have a stream ID which is used to distinguish entries from
    * separate time series. For example, amp measurements have a stream ID which
    * corresponds to a particular test schedule.
    */
  val stream: String

  /** The time at which the measurement occurred. */
  val time: Instant

  /** @return True if the object represents a measurement that encountered loss.
    *         If the type of measurement cannot record loss, this function will
    *         return false.
    */
  def isLossy: Boolean
}
