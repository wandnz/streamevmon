package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.connectors.postgres.AsNumber
import nz.net.wand.streamevmon.flink.SerializableInetAddress

import org.jgrapht.graph.GraphWalk

import scala.collection.mutable

sealed trait Host extends Serializable {
  val knownPaths: mutable.Set[GraphWalk[Host, AmpletGraphBuilder#EdgeT]] = mutable.Set()

  def sharesAddressesWith(other: Host): Boolean

  def mergeWith(other: Host): Host

  val uid: String
}

sealed trait KnownHostname {
  val hostname: String
}

class HostWithKnownHostname(
  val hostname : String,
  val addresses: Iterable[(SerializableInetAddress, AsNumber)]
) extends Host
          with KnownHostname {

  override def sharesAddressesWith(other: Host): Boolean = other match {
    case that: HostWithKnownHostname => addresses.exists(addr => that.addresses.exists(_ == addr))
    case that: HostWithUnknownHostname => addresses.toSeq.contains(that.address)
    case _: HostWithUnknownAddress => false
  }

  override def mergeWith(other: Host): Host = other match {
    case that: HostWithKnownHostname if this == that =>
      new HostWithKnownHostname(
        hostname,
        (addresses ++ that.addresses).toSet
      )
    case that: HostWithUnknownHostname if this.sharesAddressesWith(that) => this
    case _ =>
      throw new IllegalArgumentException("Can't merge hosts without shared hostname or address")
  }

  def canEqual(other: Any): Boolean =
    other.isInstanceOf[HostWithKnownHostname]

  override def equals(other: Any): Boolean = other match {
    case that: HostWithKnownHostname => (that canEqual this) && hostname == that.hostname
    case _ => false
  }

  override def hashCode(): Int = {
    hostname.hashCode
  }

  override val uid: String = hostname

  override def toString: String = hostname
}

class HostWithUnknownHostname(
  val address: (SerializableInetAddress, AsNumber)
) extends Host {

  override def sharesAddressesWith(other: Host): Boolean = other match {
    case that: HostWithKnownHostname => that.addresses.exists(_ == address)
    case that: HostWithUnknownHostname => address == that.address
    case _: HostWithUnknownAddress => false
  }

  override def mergeWith(other: Host): Host = other match {
    case that: HostWithKnownHostname if this.sharesAddressesWith(that) => that
    case that: HostWithUnknownHostname if this == that => this
    case _ => throw new IllegalArgumentException("Can't merge hosts without shared address")
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[HostWithUnknownHostname]

  override def equals(other: Any): Boolean = other match {
    case that: HostWithUnknownHostname => (that canEqual this) && address == that.address
    case _ => false
  }

  override def hashCode(): Int = {
    address.toString.hashCode
  }

  override val uid: String = address.toString

  override def toString: String = s"${address._1} (${address._2})"
}

class HostWithUnknownAddress(
  val stream     : Int,
  val pathId     : Int,
  val indexInPath: Int
) extends Host {

  override def sharesAddressesWith(other: Host): Boolean = false

  override def mergeWith(other: Host): Host = {
    if (this != other) {
      throw new IllegalArgumentException("Can't merge non-identical anonymous hosts")
    }
    this
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[HostWithUnknownAddress]

  override def equals(other: Any): Boolean = other match {
    case that: HostWithUnknownAddress =>
      (that canEqual this) &&
        stream == that.stream &&
        pathId == that.pathId &&
        indexInPath == that.indexInPath
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(stream, pathId, indexInPath)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }

  override val uid: String = s"$stream,$pathId,$indexInPath"

  override def toString: String = s"? ($uid)"
}
