package nz.net.wand.streamevmon.connectors.postgres

import org.squeryl.customtypes.StringField

case class AsPath(rawInput: String)
  extends StringField(rawInput)
          with Iterable[AsPathEntry] {
  private lazy val path: Array[AsPathEntry] = rawInput
    .drop(1)
    .dropRight(1)
    .split(",")
    .map(AsPathEntry(_))

  override def iterator: Iterator[AsPathEntry] = path.iterator

  override def canEqual(other: Any): Boolean = other.isInstanceOf[AsPath]

  override def equals(other: Any): Boolean = other match {
    case that: AsPath => (that canEqual this) && rawInput == that.rawInput
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(rawInput)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
