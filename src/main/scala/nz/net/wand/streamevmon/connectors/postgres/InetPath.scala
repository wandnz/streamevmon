package nz.net.wand.streamevmon.connectors.postgres

import java.net.InetAddress

import org.squeryl.customtypes.StringField

import scala.util.Try

/** A path taken between hosts during an AMP traceroute test. */
case class InetPath(rawInput: String)
  extends StringField(rawInput)
          with Iterable[Option[InetAddress]] {
  private val path: Array[Option[InetAddress]] = rawInput
    .drop(1)
    .dropRight(1)
    .split(",")
    .map(a => Try(InetAddress.getByName(a)).toOption)

  override def iterator: Iterator[Option[InetAddress]] = path.iterator
}
