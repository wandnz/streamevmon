package nz.net.wand.streamevmon.connectors.postgres

import java.net.InetAddress

/** One entry in an [[AsInetPath]]. `address` is optional, as the address of a
  * hop may be unknown. AsNumber can be part of any of its categories, including
  * Unknown or Missing.
  */
case class AsInetPathEntry(
  address: Option[InetAddress],
  as: AsNumber,
  lastHop       : Option[AsInetPathEntry] = None
) {
  val ampletHostname: Option[String] = None

  override def toString: String = {
    address match {
      case Some(value) => value.toString +
        s" (${as.toString})"
      case None => s"?.?.?.?" +
        s"${lastHop.map(h => s" (From $h)").getOrElse("")}"
    }
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[AsInetPathEntry]

  override def equals(other: Any): Boolean = other match {
    case that: AsInetPathEntry =>
      (that canEqual this) &&
        address == that.address &&
        as == that.as &&
        lastHop == that.lastHop
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(address, as, lastHop)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
