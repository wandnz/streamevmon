package nz.net.wand.streamevmon.connectors.postgres

import org.squeryl.customtypes.StringField

/** A Traceroute test reports a path taken in terms of IPs and ASes.
  * This class represents the AS path, in which a new entry represents
  * a transition into a new AS.
  */
case class AsPath(rawInput: String)
  extends StringField(rawInput)
          with Iterable[AsPathEntry] {
  private lazy val path: Array[AsPathEntry] = rawInput
    .drop(1)
    .dropRight(1)
    .split(",")
    .map(AsPathEntry(_))

  override def iterator: Iterator[AsPathEntry] = path.iterator
}
