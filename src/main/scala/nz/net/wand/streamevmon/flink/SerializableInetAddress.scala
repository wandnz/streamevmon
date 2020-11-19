package nz.net.wand.streamevmon.flink

import java.net.InetAddress

import scala.language.implicitConversions

case class SerializableInetAddress(
  address: Array[Byte]
) {

  import nz.net.wand.streamevmon.flink.SerializableInetAddress._

  @transient lazy val asInetAddress: InetAddress = this

  override def toString: String = asInetAddress.toString

  def canEqual(other: Any): Boolean = other.isInstanceOf[SerializableInetAddress]

  override def equals(obj: Any): Boolean = obj match {
    case that: SerializableInetAddress => (that canEqual this) && (address sameElements that.address)
    case _ => false
  }

  override def hashCode(): Int = address.hashCode
}

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
