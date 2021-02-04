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

package nz.net.wand.streamevmon

/** Contains implementations of detection algorithms. Many of these will be
  * implemented as ProcessFunctions, similarly to the
  * [[nz.net.wand.streamevmon.detectors.SimpleThresholdDetector SimpleThresholdDetector]].
  *
  * Generally, a detection algorithm will want to take some stream of
  * [[nz.net.wand.streamevmon.measurements.traits.Measurement Measurements]]
  * (keyed, windowed, or otherwise) and produce a stream of
  * [[nz.net.wand.streamevmon.events.Event Events]].
  *
  * See CONTRIBUTING.md for details.
  */
package object detectors {}
