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

/** AMP can report a few special cases that aren't AS numbers.
  * RFC 1918 declares a few IP address ranges for private use, which
  * fall under PrivateAddress. Alternatively, discovering the AS
  * number could fail and result in a Missing or Unknown result.
  */
object AsNumberCategory extends Enumeration {
  // RFC 1918 private address
  val PrivateAddress: Value = Value(-2)
  val Missing: Value = Value(-1)
  val Unknown: Value = Value(0)
  val Valid: Value = Value(1)
}
