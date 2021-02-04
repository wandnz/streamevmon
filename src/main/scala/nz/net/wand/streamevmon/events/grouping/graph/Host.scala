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

import nz.net.wand.streamevmon.connectors.postgres.schema.AsNumber

/** Represents hosts that are part of a Traceroute path.
  *
  * There are several tiers of potential knowledge about each host:
  *
  * - If we know the hostname, we are sure of its identity. We may know any
  * number (including zero) of its InetAddresses.
  * - If we don't know the hostname, but we do know of an InetAddress, we have
  * a reasonable unique identifier. If we later find out a hostname that owns
  * this address, we can move this to the first tier.
  * - If we don't know a hostname or an InetAddress, the host is essentially
  * completely unknown. We can distinguish it from other unknown hosts by
  * using the stream that the Traceroute measurement was part of, as well as
  * the path ID we found the host in, and its index within the path. These
  * can never be merged with higher tiers, since we have no knowledge to merge.
  *
  * Each address is paired with an
  * [[nz.net.wand.streamevmon.connectors.postgres.schema.AsNumber AsNumber]],
  * which can be missing or unknown.
  *
  * More advanced methods of host deduplication may reveal a fourth case, where
  * multiple hostnames are known.
  */
sealed trait Host extends Serializable {

  /** Returns true if one hosts shares addresses with another. */
  def sharesAddressesWith(other: Host): Boolean

  /** Merges two hosts, such that the hostname is preserved, as well as all
    * addresses in both Hosts. If no merge is possible, an
    * IllegalArgumentException is thrown. Note that merging two
    * HostWithKnownAddresses is not yet supported.
    */
  def mergeWith(other: Host): Host

  /** The existing equals() implementation mostly has traits derived from
    * convenience when it comes to placing Hosts into a graph from JGraphT.
    * This function tests if two hosts are really truly equal, without ignoring
    * any fields or taking any shortcuts.
    */
  def deepEquals(other: Any): Boolean = this.equals(other)

  /** A unique identifier for a host. Each implementation defines its own
    * format for this field.
    */
  val uid: String
}

/** This is the case where we know one hostname of a host. A host can have
  * several unique addresses.
  */
class HostWithKnownHostname(
  val hostname: String,
  val addresses: Set[(SerializableInetAddress, AsNumber)]
) extends Host {

  override def sharesAddressesWith(other: Host): Boolean = other match {
    case that: HostWithKnownHostname => addresses.exists(addr => that.addresses.contains(addr))
    case that: HostWithUnknownHostname => addresses.toSeq.contains(that.address)
    case _: HostWithUnknownAddress => false
  }

  override def mergeWith(other: Host): Host = other match {
    // This uses the simple equals() test that only checks hostname. The address
    // sets can be merged simply, since they're sets.
    case that: HostWithKnownHostname if this == that =>
      new HostWithKnownHostname(
        hostname,
        addresses ++ that.addresses
      )
    // The other side only has one hostname, so if we already have it, we can
    // just do nothing.
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

  override def deepEquals(other: Any): Boolean = other match {
    case that: HostWithKnownHostname =>
      (that canEqual this) &&
        hostname == that.hostname &&
        // Turns out list equality in Scala is kinda stupid.
        // List(1,2) != Set(1,2), and order matters in other cases.
        // We solve this by just making sure everything is a Set, and then
        // changing them to a type that has a reasonable equality method.
        addresses.toSeq == that.addresses.toSeq
  }

  override def hashCode(): Int = {
    hostname.hashCode
  }

  override val uid: String = hostname

  override def toString: String = hostname
}

/** If we don't know a host's hostname, then we can only ever know a single one
  * of its addresses. This can get merged in with a HostWithKnownHostname later
  * if we discover some overlap.
  */
class HostWithUnknownHostname(
  val address: (SerializableInetAddress, AsNumber)
) extends Host {

  override def sharesAddressesWith(other: Host): Boolean = other match {
    case that: HostWithKnownHostname => that.addresses.contains(address)
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

/** If we know no addresses, we are reduced to this UID tuple. It can't be
  * merged with any other hosts unless they are identical.
  */
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
