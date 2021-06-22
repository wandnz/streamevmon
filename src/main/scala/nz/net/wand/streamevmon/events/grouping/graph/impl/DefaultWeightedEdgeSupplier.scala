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

package nz.net.wand.streamevmon.events.grouping.graph.impl

import java.util.function.Supplier

import org.jgrapht.graph.DefaultWeightedEdge

/** The built-in default edge supplier uses reflection to generate an instance
  * of the type parameter passed to the graph. Kryo refuses to serialise this
  * in JDK 9+ due to the new module system preventing reflective access to
  * certain Java core modules. Bypassing the reflective solution is a much
  * simpler approach than opening the module to reflective access.
  */
class DefaultWeightedEdgeSupplier extends Supplier[DefaultWeightedEdge] with Serializable {
  override def get(): DefaultWeightedEdge = new DefaultWeightedEdge()
}
