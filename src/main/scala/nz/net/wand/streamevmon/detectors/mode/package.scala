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

/** Mode detector. Emits events when the value that occurs the most often in
  * recent events changes significantly. This detector will re-scale the values
  * of measurements in order to perform some bucketing before mesauring modes.
  *
  * Written by Daniel Oosterwijk, and is a close parallel to the mode detector
  * included in netevmon, which was written by Shane Alcock and Brendon Jones.
  *
  * There are a few classes which are private to this package. They were
  * originally inner classes of ModeDetector, but Kryo does not like serialising
  * inner classes, which was causing issues.
  *
  * ==Configuration==
  *
  * This package is configured by the `detector.mode` config key group.
  *
  * - `maxHistory`: The number of recent measurements to retain and consider.
  * Default 30.
  *
  * - `minFrequency`: The minimum number of times a particular value must occur
  * in recent history to be considered for an event.
  * Default 6.
  *
  * - `minProminence`: The minimum difference between the number of instances of
  * the primary and secondary modes in recent history for the primary mode to
  * be considered for an event.
  * Default 3.
  *
  * - `threshold`: The minimum threshold value used when considering the absolute
  * difference between the values of the old mode and the new mode. This may be
  * higher in practice, since the detector automatically calculates a threshold
  * and takes the minimum of the two values.
  * Default 7.5.
  *
  * - `inactivityPurgeTime`: The amount of time between measurements, in seconds,
  * before history is purged.
  * Default 60.
  *
  */
package object mode {}
