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

import java.net.InetAddress

import scala.language.implicitConversions

/** InetAddress can't be serialized by Kryo, meaning we can't use it in any
  * situation that it would be serialized by Flink, such as as a field in an
  * operator output, or as part of the persistent state of an operator.
  *
  * This class works around that issue by being a transparent standin for an
  * InetAddress, complete with implicit conversions. While it does not store
  * all the persistent fields of an InetAddress, such as the hostname
  * functionality, we only use the address-storage part of InetAddress.
  *
  * An alternative to this class would be to use the library which provides an
  * IpAddress class, which explicitly supports Kryo serialization.
  */
case class SerializableInetAddress(
  address: Array[Byte]
) {

  // We'll use the implicit conversions here for simplicity.

  // Since this object is immutable, we can use a val instead of a def for
  // explicit conversions of this format.
  @transient lazy val asInetAddress: InetAddress = this

  override def toString: String = asInetAddress.toString

  def canEqual(other: Any): Boolean = other.isInstanceOf[SerializableInetAddress]

  override def equals(obj: Any): Boolean = obj match {
    case that: SerializableInetAddress => (that canEqual this) && (address sameElements that.address)
    case _ => false
  }

  override def hashCode(): Int = address.hashCode
}

/** Implicit conversions between SerializableInetAddress and InetAddress.
  * Includes conversions to and from for the raw objects and for Options of them.
  */
object SerializableInetAddress {
  implicit def inetToSerializable(inet: InetAddress): SerializableInetAddress = {
    SerializableInetAddress(inet.getAddress)
  }

  implicit def serializableToInet(serializable: SerializableInetAddress): InetAddress = {
    InetAddress.getByAddress(serializable.address)
  }

  implicit def optionInetToOptionSerializable(inet: Option[InetAddress]): Option[SerializableInetAddress] = {
    inet.map(inetToSerializable)
  }

  implicit def optionSerializableToOptionInet(serializable: Option[SerializableInetAddress]): Option[InetAddress] = {
    serializable.map(serializableToInet)
  }
}
