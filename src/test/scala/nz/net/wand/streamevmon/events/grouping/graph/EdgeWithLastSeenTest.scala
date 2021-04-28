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

import nz.net.wand.streamevmon.events.grouping.graph.impl.EdgeWithLastSeen
import nz.net.wand.streamevmon.test.TestBase

import java.time.Instant

import com.esotericsoftware.kryo.io.{ByteBufferInput, ByteBufferOutput}
import com.twitter.chill.ScalaKryoInstantiator

class EdgeWithLastSeenTest extends TestBase {
  "EdgeWithLastSeen" should {
    "serialize via Kryo" in {
      val time = Instant.now()
      val original = new EdgeWithLastSeen(time, "uid")

      val inst = new ScalaKryoInstantiator()
      inst.setRegistrationRequired(false)
      val k = inst.newKryo()

      val holder = new ByteBufferOutput(1024)
      k.writeClassAndObject(holder, original)
      val input = new ByteBufferInput(holder.getByteBuffer)
      input.setPosition(0)
      val output = k.readClassAndObject(input)
      output shouldBe an[EdgeWithLastSeen]
      output.asInstanceOf[EdgeWithLastSeen].lastSeen shouldBe time
    }

    val a = new EdgeWithLastSeen(Instant.EPOCH, "a")
    val aa = new EdgeWithLastSeen(Instant.EPOCH, "a")
    val b = new EdgeWithLastSeen(Instant.EPOCH, "b")
    val c = new EdgeWithLastSeen(Instant.ofEpochMilli(1000), "a")
    val d = new EdgeWithLastSeen(Instant.ofEpochMilli(1000), "b")

    "have good equals()" in {
      assert(a.equals(aa))
      assert(!a.equals(b))
      assert(!a.equals(c))
      assert(!a.equals(d))
    }

    "have good hashCode()" in {
      a.hashCode shouldBe aa.hashCode
      a.hashCode shouldNot be(b.hashCode())
      a.hashCode shouldNot be(c.hashCode())
      a.hashCode shouldNot be(d.hashCode())
    }
  }
}
