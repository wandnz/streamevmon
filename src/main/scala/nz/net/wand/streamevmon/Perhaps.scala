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

/** Allows for optional implicit evidence of additional type parameters.
  *
  * @see [[nz.net.wand.streamevmon.runners.unified.schema.DetectorType.ValueBuilder DetectorType.ValueBuilder]] for example usage
  * @see [[https://stackoverflow.com/a/20016152]] for source
  */
case class Perhaps[E](value: Option[E]) {
  lazy val isDefined: Boolean = value.isDefined
}

object Perhaps {
  implicit def perhaps[F](implicit ev: F = null): Perhaps[F] = Perhaps(Option(ev))
}
