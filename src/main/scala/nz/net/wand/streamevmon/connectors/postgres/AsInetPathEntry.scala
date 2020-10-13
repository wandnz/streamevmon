package nz.net.wand.streamevmon.connectors.postgres

import java.net.InetAddress

/** One entry in an [[AsInetPath]]. `address` is optional, as the address of a
  * hop may be unknown. AsNumber can be part of any of its categories, including
  * Unknown or Missing.
  */
case class AsInetPathEntry(address: Option[InetAddress], as: AsNumber) {
  override def toString: String = {
    s"${
      address match {
        case Some(value) => value.toString
        case None => "?.?.?.?"
      }
    }" +
      s" (${as.toString})"
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[AsInetPathEntry]

  override def equals(other: Any): Boolean = other match {
    case that: AsInetPathEntry =>
      (that canEqual this) &&
        address == that.address &&
        as.equals(that.as)
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(address)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
