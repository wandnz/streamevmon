package nz.net.wand.streamevmon.connectors.postgres

import java.net.InetAddress

/** One entry in an [[AsInetPath]]. `address` is optional, as the address of a
  * hop may be unknown. AsNumber can be part of any of its categories, including
  * Unknown or Missing.
  */
case class AsInetPathEntry(
  address: Option[InetAddress],
  as   : AsNumber,
  ampletHostname: Option[String] = None
) {
  override def toString: String = {
    s"${
      address match {
        case Some(value) => value.toString
        case None => "?.?.?.?"
      }
    }" +
      s"${ampletHostname.map(n => s" ($n)").getOrElse("")}" +
      s" (${as.toString})"
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[AsInetPathEntry]

  override def equals(other: Any): Boolean = other match {
    case that: AsInetPathEntry =>
      (that canEqual this) &&
        address == that.address &&
        as == that.as &&
        ampletHostname == that.ampletHostname
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(address, as, ampletHostname)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
