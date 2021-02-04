/* This file is part of streamevmon.
 *
 * Copyright (C) 2020-2021  The University of Waikato, Hamilton, New Zealand
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
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.net.wand.streamevmon

/** This package contains the optional parameterTuner module. It uses SMAC2 to
  * generate new sets of parameters according to some method of scoring results.
  *
  * The main entrypoint is [[nz.net.wand.streamevmon.tuner.ParameterTuner ParameterTuner]].
  *
  * The `jobs` and `strategies` packages contain logic for parameter tuning
  * independent from SMAC. The `nab` package contains logic for using the NAB
  * benchmark to get scores from a particular algorithm run. The `nab.smac`
  * package contains the glue between the NAB scorer and SMAC.
  *
  * Currently, only NAB is supported.
  */
package object tuner {}
