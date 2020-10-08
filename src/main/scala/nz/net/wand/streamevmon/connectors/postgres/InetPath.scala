package nz.net.wand.streamevmon.connectors.postgres

import java.net.InetAddress

import org.squeryl.customtypes.StringField

import scala.util.Try

class InetPath(rawInput: String)
  extends StringField(rawInput)
          with Iterable[Option[InetAddress]] {
  private val path: Array[Option[InetAddress]] = rawInput
    .drop(1)
    .dropRight(1)
    .split(",")
    .map(a => Try(InetAddress.getByName(a)).toOption)

  override def iterator: Iterator[Option[InetAddress]] = path.iterator

  override def canEqual(other: Any): Boolean = other.isInstanceOf[InetPath]

  override def equals(other: Any): Boolean = other match {
    case that: InetPath => (that canEqual this) && rawInput == that.rawInput
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(rawInput)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
