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

package nz.net.wand.streamevmon.runners.unified.schema

import com.fasterxml.jackson.module.scala.JsonScalaEnumeration

/** Refers to a particular stream from a particular source.
  *
  * @param name        The source's user-defined name. This is the key in the [[FlowSchema]]'s
  *                    `sources` field which points to the desired SourceInstance.
  * @param datatype    The datatype desired from the source.
  * @param filterLossy Whether or not to remove lossy measurements from the typed stream.
  */
case class SourceReference(
  name : String,
  @JsonScalaEnumeration(classOf[SourceDatatypeReference])
  datatype: SourceDatatype.Value,
  filterLossy: Boolean
)
