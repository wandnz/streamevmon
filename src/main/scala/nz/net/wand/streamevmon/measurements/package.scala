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

/** Contains classes representing network measurements, such as those gathered
  * by AMP or perfSONAR Esmond.
  *
  * Measurements are grouped into categories based on their source. Each
  * category has its own package, and the package objects have further
  * documentation.
  *
  * Any measurement can inherit from one or more of the items in the [[nz.net.wand.streamevmon.measurements.traits `traits`]]
  * package. These signal additional functionality, and as such many detectors
  * require received messages to implement particular traits. The most notable
  * is [[nz.net.wand.streamevmon.measurements.traits.HasDefault HasDefault]],
  * which allows a measurement to provide a single default Double value. See
  * the package object for more details.
  *
  * All measurements should inherit from
  * [[nz.net.wand.streamevmon.measurements.traits.Measurement Measurement]], or
  * one of its more specific subtypes. See the documentation for Measurement for
  * some notes on what requirements must be implemented.
  */
package object measurements {}
