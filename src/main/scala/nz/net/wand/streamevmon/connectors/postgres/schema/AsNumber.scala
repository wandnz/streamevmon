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

/** An Autonomous System Number. Supports the special cases of
  * AS "numbers" that amp might report, such as private IPs or
  * missing AS numbers.
  */
case class AsNumber(private val num: Int) {
  // We could use AsNumberCategory(num), but this would throw
  // an exception for all valid ASes except ID 1. We want to
  // avoid the cost involved with an exception.
  val category: AsNumberCategory.Value = num match {
    case -2 => AsNumberCategory.PrivateAddress
    case -1 => AsNumberCategory.Missing
    case 0 => AsNumberCategory.Unknown
    case _ => AsNumberCategory.Valid
  }

  val number: Option[Int] = category match {
    case AsNumberCategory.Valid => Some(num)
    case _ => None
  }

  override def toString: String = category match {
    case AsNumberCategory.Unknown => "AS Unknown"
    case AsNumberCategory.Missing => "AS Missing"
    case AsNumberCategory.PrivateAddress => "Private Address"
    case AsNumberCategory.Valid => s"AS $num"
  }
}

object AsNumber {
  val PrivateAddress: AsNumber = AsNumber(AsNumberCategory.PrivateAddress.id)
  val Missing: AsNumber = AsNumber(AsNumberCategory.Missing.id)
  val Unknown: AsNumber = AsNumber(AsNumberCategory.Unknown.id)
}
