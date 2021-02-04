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

package nz.net.wand.streamevmon.detectors

/** This package contains the baseline detector module. It accepts any type of
  * measurement which includes HasDefault, and outputs Baseline Events.
  *
  * ==Configuration==
  *
  * This package is configured by the `detector.baseline` config key group.
  *
  * - `maxHistory`: The maximum number of measurements to retain. This can
  * affect how sensitive the detector is to changes that do not last long.
  * Default 50.
  *
  * - `percentile`: Which percentile of the measurement window to check for
  * changes. For example, a value of 0.50 is the median, while 0.75 is the value
  * which is larger than 75% of the other values in the window.
  * Default 0.10.
  *
  * - `threshold`: The change severity threshold that must be exceeded for an
  * event to be emitted. This value should be between 0 and 100.
  * Default 25.
  *
  * - `inactivityPurgeTime`: If the gap between two successive measurements is
  * this many seconds or more, we should drop all our history and start fresh.
  * Default 600.
  *
  */
package object baseline {}
