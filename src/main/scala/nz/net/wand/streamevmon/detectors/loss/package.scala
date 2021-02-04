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

/** Simple loss detector. Emits events when a certain proportion of recent
  * values are lossy. This detector is best used on streams you expect to barely
  * ever have loss, since it is naive, sensitive, and noisy.
  *
  * Written by Daniel Oosterwijk, and loosely based on the loss detector
  * included in netevmon, which was written by Shane Alcock and Brendon Jones.
  *
  * ==Configuration==
  *
  * This package is configured by the `detector.loss` config key group.
  *
  * - `maxHistory`: The maximum number of measurements to retain.
  * Default 30.
  *
  * - `lossCount`: The number of lossy measurements that must occur within the
  * last maxHistory number of measurements before an event is emitted.
  * Default 10.
  *
  * - `consecutiveCount`: The number of consecutive lossy measurements before
  * an event is emitted.
  * Default 5.
  */
package object loss {}
