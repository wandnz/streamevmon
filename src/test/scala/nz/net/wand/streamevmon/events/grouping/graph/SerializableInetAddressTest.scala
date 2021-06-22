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

import nz.net.wand.streamevmon.events.grouping.graph.impl.SerializableInetAddress
import nz.net.wand.streamevmon.test.TestBase

import java.io._
import java.net.InetAddress

import com.esotericsoftware.kryo.io.{ByteBufferInput, ByteBufferOutput}
import com.twitter.chill.ScalaKryoInstantiator

class SerializableInetAddressTest extends TestBase {
  "SerializableInetAddress" should {
    "be serializable" when {
      "using Java serialization" in {
        val original = SerializableInetAddress(Array(15, 20, 25, 30))
        val serialized = new ByteArrayOutputStream()
        new ObjectOutputStream(serialized).writeObject(original)
        val deserialized = new ObjectInputStream(new ByteArrayInputStream(serialized.toByteArray)).readObject()

        deserialized shouldBe original
      }

      "using Kryo" in {
        val original = SerializableInetAddress(Array(15, 20, 25, 30))
        val inst = new ScalaKryoInstantiator()
        inst.setRegistrationRequired(false)
        val k = inst.newKryo()

        val holder = new ByteBufferOutput(1024)
        k.writeClassAndObject(holder, original)
        val input = new ByteBufferInput(holder.getByteBuffer)
        input.setPosition(0)
        val output = k.readClassAndObject(input)
        output shouldBe original
      }
    }

    "be interchangeable with InetAddress" when {
      "an InetAddress is provided" in {
        SerializableInetAddress.inetToSerializable(
          InetAddress.getByAddress(Array(1, 2, 3, 4))
        ) shouldBe SerializableInetAddress(Array(1, 2, 3, 4))

        SerializableInetAddress.optionInetToOptionSerializable(
          Some(InetAddress.getByAddress(Array(1, 2, 3, 4)))
        ) shouldBe Some(SerializableInetAddress(Array(1, 2, 3, 4)))

        SerializableInetAddress.optionInetToOptionSerializable(None) shouldBe None
      }

      "a SerializableInetAddress is provided" in {
        SerializableInetAddress.serializableToInet(
          SerializableInetAddress(Array(1, 2, 3, 4))
        ) shouldBe InetAddress.getByAddress(Array(1, 2, 3, 4))

        SerializableInetAddress.optionSerializableToOptionInet(
          Some(SerializableInetAddress(Array(1, 2, 3, 4)))
        ) shouldBe Some(InetAddress.getByAddress(Array(1, 2, 3, 4)))

        SerializableInetAddress.optionSerializableToOptionInet(None) shouldBe None
      }
    }
  }
}
