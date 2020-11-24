package nz.net.wand.streamevmon.flink

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

  import nz.net.wand.streamevmon.flink.SerializableInetAddress._

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
