package nz.net.wand.streamevmon.connectors.postgres

import java.net.InetAddress

/** One entry in an [[AsInetPath]]. `address` is optional, as the address of a
  * hop may be unknown. AsNumber can be part of any of its categories, including
  * Unknown or Missing.
  */
case class AsInetPathEntry(address: Option[InetAddress], as: AsNumber)
