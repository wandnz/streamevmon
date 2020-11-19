package nz.net.wand.streamevmon.connectors.postgres

import nz.net.wand.streamevmon.flink.SerializableInetAddress

/** One entry in an [[AsInetPath]]. `address` is optional, as the address of a
  * hop may be unknown. AsNumber can be part of any of its categories, including
  * Unknown or Missing.
  */
case class AsInetPathEntry(
  address: Option[SerializableInetAddress],
  as     : AsNumber
) {

  override def toString: String = {
    address match {
      case Some(value) => value.toString + s" (${as.toString})"
      case None => s"?.?.?.?"
    }
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[AsInetPathEntry]

  override def equals(other: Any): Boolean = other match {
    case that: AsInetPathEntry =>
      (that canEqual this) &&
        address == that.address &&
        as == that.as
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(address, as)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
