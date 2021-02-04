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

import com.esotericsoftware.kryo.io.{Input, Output}
import com.twitter.chill.ScalaKryoInstantiator
import scalacache.serialization.{Codec, FailedToDecode}
import scalacache.serialization.gzip.GZippingBinaryCodec
import scalacache.serialization.Codec.DecodingResult

import scala.util.{Failure, Success, Try}

/** A serialization codec to use with Scalacache that uses Kryo as a backend,
  * since it's what Flink uses and it's better than Java serialization.
  */
class KryoCodec extends Codec[Any] {
  @transient private val kryo = new ScalaKryoInstantiator().newKryo()

  override def encode(value: Any): Array[Byte] = {
    // We start with a 4K buffer, but let it resize infinitely.
    val output = new Output(4096, -1)
    kryo.writeClassAndObject(output, value)
    output.getBuffer
  }

  override def decode(data: Array[Byte]): DecodingResult[Any] = {
    val input = new Input(data)
    Try(kryo.readClassAndObject(input)) match {
      case Failure(exception) => Left(FailedToDecode(exception))
      case Success(value) => if (value == null) {
        Left(FailedToDecode(new IllegalArgumentException("Result of Kryo decoding was null!")))
      }
      else {
        Right(value)
      }
    }
  }
}

/** Same as [[KryoCodec]], but applies GZip for items larger than a certain
  * threshold.
  */
class GZippingKryoCodec extends KryoCodec with GZippingBinaryCodec[Any]
