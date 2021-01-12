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

import nz.net.wand.streamevmon.connectors.postgres.schema.{AsNumber, AsNumberCategory}

/** Represents known single hosts in a network. Each host might have several
  * hostnames and several addresses. We do our best to use this class in such
  * a way that one Host represents one physical Host, but it is not a guarantee.
  *
  * If either `hostnames` or `addresses` contains at least one entry, then
  * `ampTracerouteUid` should be None. If both `hostnames` and `addresses` are
  * empty, then `ampTracerouteUid` should be defined.
  *
  * If `itdkNodeId` is defined, then either `hostnames` or `addresses` should
  * contain at least one entry.
  *
  * An IllegalArgumentException will be thrown on construction if any of the
  * above constraints are violated.
  */
case class Host(
  hostnames: Set[String],
  addresses: Set[(SerializableInetAddress, AsNumber)],
  ampTracerouteUids: Set[(Int, Int, Int)],
  itdkNodeId: Option[(Int, AsNumber)]
) extends Serializable {

  // Sanity checks
  if (ampTracerouteUids.nonEmpty) {
    if (hostnames.nonEmpty || addresses.nonEmpty) {
      throw new IllegalArgumentException("Traceroute UID is defined, but a hostname or address is known!")
    }
  }
  else {
    if (hostnames.isEmpty && addresses.isEmpty) {
      throw new IllegalArgumentException("Traceroute UID is undefined, but no hostnames or addresses are known!")
    }
  }

  if (itdkNodeId.isDefined) {
    if (hostnames.isEmpty && addresses.isEmpty) {
      throw new IllegalArgumentException("ITDK node ID is defined, but no hostnames or addresses are known!")
    }
  }

  /** A set of all AsNumbers associated with the addresses and ITDK node ID of
    * this object.
    */
  lazy val allAsNumbers: Set[AsNumber] = {
    addresses.map(_._2) ++ itdkNodeId.map(_._2)
  }

  /** A set of all valid AsNumbers associated with the addresses and ITDK node
    * ID of this object. This does not include Missing, Unknown, or
    * PrivateAddress AsNumbers.
    */
  lazy val validAsNumbers: Set[AsNumber] = {
    allAsNumbers.filter(_.category == AsNumberCategory.Valid)
  }

  /** The ITDK AS number, if defined and valid, otherwise one of the valid
    * numbers from `addresses`. Used if a single consistent AS number is
    * required, like for toString and exporting.
    */
  lazy val primaryAsNumber: Option[AsNumber] = {
    (Seq(itdkNodeId.map(_._2).getOrElse(AsNumber.Missing)) ++ addresses.map(_._2))
      .find(_.category == AsNumberCategory.Valid)
  }

  /** A unique identifier for each node, which should be used as the key for
    * Maps containing these Hosts. It is implemented separately from hashCode
    * because fulfilling the contract for both hashCode and equals is not
    * possible with the behaviour we can achieve here.
    *
    * The UID follows a hierarchy depending on available information:
    * - If the ITDK node ID is present, it is used.
    * - If any hostnames are present, they are separated by ';' and used.
    * - If any addresses are present, they are separated by ';' and used.
    * - If nothing else is present, the ampTracerouteUid is used.
    */
  val uid: String = {
    itdkNodeId match {
      case Some(value) => s"N$value"
      case None => if (hostnames.nonEmpty) {
        hostnames.mkString(";")
      }
      else if (addresses.nonEmpty) {
        addresses.mkString(";")
      }
      else {
        if (ampTracerouteUids.isEmpty) {
          throw new IllegalStateException("Trying to get UID for host with no data!")
        }
        ampTracerouteUids.mkString(";")
      }
    }
  }

  /** A user-friendly and unique (per `uid`) name for the Host. This does not
    * need to contain all the information we have about the host, just enough
    * to differentiate it and describe it.
    *
    * - If there are any hostnames, one of them becomes the name of the node. If
    * there are multiple, it is followed by "(and `n` more)".
    * - If there are no hostnames, but there are addresses, the same format is
    * followed with an address.
    * - If there are no hostnames or addresses, the name is
    * "? (`ampTracerouteUid`)".
    * - Regardless of what other information is available, if the ITDK node ID
    * is known, it is appended to the name in the format "(ITDK N12345)".
    */
  override def toString: String = {
    s"""${
      if (hostnames.nonEmpty) {
        s"${hostnames.head}${
          if (hostnames.size > 1) {
            s" (and ${hostnames.size - 1} more)"
          }
          else {
            ""
          }
        } (${addresses.size} addresses)"
      }
      else if (addresses.nonEmpty) {
        s"${addresses.head._1}${
          if (addresses.size > 1) {
            s" (and ${addresses.size - 1} more)"
          }
          else {
            ""
          }
        } (${primaryAsNumber.getOrElse(AsNumber.Unknown)}${
          if (allAsNumbers.size > 1) {
            s" and ${allAsNumbers.size - 1} more"
          }
          else {
            ""
          }
        })"
      }
      else {
        s"? ${ampTracerouteUids.mkString(",")}".trim
      }
    }${itdkNodeId.map(id => s" (ITDK N${id._1}, ${id._2})").getOrElse("")}"""
  }

  /** @return True if both hosts have identical hostname lists. */
  def equalHostnames(other: Host): Boolean = this.hostnames == other.hostnames

  /** @return True if both hosts share any hostnames. */
  def sharesHostnamesWith(other: Host): Boolean = {
    this.hostnames.exists(name => other.hostnames.contains(name))
  }

  /** @return True if both hosts have identical address lists. */
  def equalAddresses(other: Host): Boolean = this.addresses == other.addresses

  /** @return True if both hosts share any addresses. */
  def sharesAddressesWith(other: Host): Boolean = {
    this.addresses.exists(addr => other.addresses.contains(addr))
  }

  /** @return True if both hosts have ITDK node IDs, and they are the same. */
  def equalItdkNode(other: Host): Boolean = {
    itdkNodeId.isDefined && this.itdkNodeId == other.itdkNodeId
  }

  /** @return True if both hosts have identical AMP traceroute UID lists. */
  def equalAmpTracerouteUids(other: Host): Boolean = this.ampTracerouteUids == other.ampTracerouteUids

  /** @return True if both hosts share any AMP traceroute UIDs. */
  def sharesAmpTracerouteUidsWith(other: Host): Boolean = {
    this.ampTracerouteUids.exists(addr => other.ampTracerouteUids.contains(addr))
  }

  /** Merges this host with the other host, and returns the result. The
    * following behaviours are followed:
    *
    * - If the ITDK node IDs are both present and do not match, an
    * IllegalArgumentException is thrown. Otherwise, the node ID is preserved.
    * - Hostname and address lists are merged.
    * - If there are no hostnames, addresses, or ITDK node IDs, the AMP
    * traceroute UIDs are compared. If they differ, an
    * IllegalArgumentException is thrown. If they are the same, it is
    * preserved.
    */
  def mergeWith(other: Host): Host = {
    val newItdkNodeId = if (
      this.itdkNodeId.isDefined &&
        other.itdkNodeId.isDefined &&
        this.itdkNodeId != other.itdkNodeId
    ) {
      throw new IllegalArgumentException(s"ITDK Node IDs do not match! ${this.itdkNodeId.get} != ${other.itdkNodeId.get}")
    }
    else {
      Seq(this.itdkNodeId, other.itdkNodeId).flatten.headOption
    }

    val newHostnames = this.hostnames ++ other.hostnames
    val newAddresses = this.addresses ++ other.addresses

    val newTracerouteUids: Set[(Int, Int, Int)] = if (
      newHostnames.isEmpty &&
        newAddresses.isEmpty &&
        newItdkNodeId.isEmpty
    ) {
      if (!this.sharesAmpTracerouteUidsWith(other)) {
        throw new IllegalArgumentException("Trying to merge two anonymous hosts with different traceroute UIDs!")
      }
      else {
        this.ampTracerouteUids ++ other.ampTracerouteUids
      }
    }
    else {
      Set()
    }

    Host(
      newHostnames,
      newAddresses,
      newTracerouteUids,
      newItdkNodeId
    )
  }

  def mergeAnonymous(other: Host): Host = {
    if (this.ampTracerouteUids.isEmpty || other.ampTracerouteUids.isEmpty) {
      throw new IllegalArgumentException(s"Told to merge anonymous hosts, but hosts weren't anonymous! $this, $other")
    }

    Host(
      hostnames,
      addresses,
      ampTracerouteUids ++ other.ampTracerouteUids,
      itdkNodeId
    )
  }
}
