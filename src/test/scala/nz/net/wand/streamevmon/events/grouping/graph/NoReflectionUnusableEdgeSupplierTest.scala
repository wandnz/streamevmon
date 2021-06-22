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

import nz.net.wand.streamevmon.events.grouping.graph.impl.NoReflectionUnusableEdgeSupplier
import nz.net.wand.streamevmon.test.TestBase

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{ByteBufferInput, ByteBufferOutput}

class NoReflectionUnusableEdgeSupplierTest extends TestBase {
  "EdgeWithLastSeenSupplier" should {
    "fail if get() is called" in {
      an[IllegalCallerException] shouldBe thrownBy(new NoReflectionUnusableEdgeSupplier().get())
    }

    "serialize via Kryo" in {
      val original = new NoReflectionUnusableEdgeSupplier[Any]()
      val k = new Kryo()
      val holder = new ByteBufferOutput(1024)
      k.writeClassAndObject(holder, original)
      val input = new ByteBufferInput(holder.getByteBuffer)
      input.setPosition(0)
      k.readClassAndObject(input) shouldBe a[NoReflectionUnusableEdgeSupplier[_]]
    }
  }
}
