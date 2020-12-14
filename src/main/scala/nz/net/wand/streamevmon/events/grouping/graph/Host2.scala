package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.connectors.postgres.schema.AsNumber

case class Host2(
  hostnames       : Set[String],
  addresses       : Set[(SerializableInetAddress, AsNumber)],
  ampTracerouteUid: Option[(Int, Int, Int)],
  itdkNodeId      : Option[Int]
) extends Serializable {

  if (ampTracerouteUid.isDefined) {
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
        ampTracerouteUid match {
          case Some(value) => value.toString
          case None => throw new IllegalStateException("Trying to get UID for host with no data!")
        }
      }
    }
  }

  override def toString: String = {
    s"${
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
        } (${addresses.head._2})"
      }
      else {
        ampTracerouteUid.map(u => s"? $u").getOrElse("Unknown Host")
      }
    }${itdkNodeId.map(id => s" (ITDK N$id)").getOrElse("")}"
  }

  def equalHostnames(other: Host2): Boolean = this.hostnames == other.hostnames

  def sharesHostnamesWith(other: Host2): Boolean = {
    this.hostnames.exists(name => other.hostnames.contains(name))
  }

  def equalAddresses(other: Host2): Boolean = this.addresses == other.addresses

  def sharesAddressesWith(other: Host2): Boolean = {
    this.addresses.exists(addr => other.addresses.contains(addr))
  }

  def equalItdkNode(other: Host2): Boolean = {
    itdkNodeId.isDefined && this.itdkNodeId == other.itdkNodeId
  }

  def mergeWith(other: Host2): Host2 = {
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

    val newTracerouteUid = if (
      newHostnames.isEmpty &&
        newAddresses.isEmpty &&
        newItdkNodeId.isEmpty
    ) {
      val uids = Set(this.ampTracerouteUid, other.ampTracerouteUid).flatten
      if (uids.size != 1) {
        throw new IllegalArgumentException("Trying to merge two anonymous hosts with different traceroute UIDs!")
      }
      else {
        this.ampTracerouteUid
      }
    }
    else {
      None
    }

    Host2(
      newHostnames,
      newAddresses,
      newTracerouteUid,
      newItdkNodeId
    )
  }
}
