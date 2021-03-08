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

package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.test.TestBase

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{ByteBufferInput, ByteBufferOutput}
import org.jgrapht.graph.DefaultWeightedEdge

class DefaultWeightedEdgeSupplierTest extends TestBase {
  // We can't run any equality tests here. Neither DefaultWeightedEdge or the
  // Supplier implement equals(). Second, the fields that we'd want to test on
  // the edge are private, and the supplier just doesn't have any fields.
  "DefaultWeightedEdgeSupplier" should {
    "create a DefaultWeightedEdge" in {
      new DefaultWeightedEdgeSupplier().get() shouldBe a[DefaultWeightedEdge]
    }

    "serialize via Kryo" in {
      val original = new DefaultWeightedEdgeSupplier()
      val k = new Kryo()
      val holder = new ByteBufferOutput(1024)
      k.writeClassAndObject(holder, original)
      val input = new ByteBufferInput(holder.getByteBuffer)
      input.setPosition(0)
      k.readClassAndObject(input) shouldBe a[DefaultWeightedEdgeSupplier]
    }
  }
}
