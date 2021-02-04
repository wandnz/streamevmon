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

/** The Distribution Difference detector keeps track of two sequential sets of
  * recent measurements. When it determines that the measurements form
  * distributions which are significantly different, it emits an event.
  *
  * Written by Daniel Oosterwijk, and is a close parallel to the detector
  * included in netevmon, which was written by Shane Alcock and Brendon Jones.
  *
  * This package includes two versions of the detector: One takes a keyed stream,
  * and the other takes a windowed keyed stream. They are configured identically.
  * When using the UnifiedRunner or the DistDiffRunner, the configuration option
  * `detector.distdiff.useFlinkWindow` can be used to choose the windowed variant
  * (true) or the sequential variant (false).
  *
  * ==Configuration==
  *
  * This package is configured by the `detector.distdiff` config key group.
  *
  * - `recentsCount`: The number of recent measurements to retain and consider.
  * Default 20.
  *
  * - `minimumChange`: The amount of proportional change between the sums of the
  * distributions that must be observed before an event is considered. For
  * example, the default value of 1.05 indicates a 5% minimum difference.
  *
  * - `zThreshold`: The minimum z-value indicating distribution difference that
  * must be observed before an event is considered. This number doesn't appear
  * to have much absolute meaning, but it is useful for tuning sensitivity.
  * Default 5.0.
  *
  * - `dropExtremeN`: When analysing the two sets of measurements, they are first
  * sorted, then this many entries are dropped from the high and low ends in
  * order to ignore outliers.
  * Default 2.
  *
  * - `inactivityPurgeTime`: The amount of time between measurements, in seconds,
  * before history is purged.
  * Default 1200s.
  *
  */
package object distdiff {}
