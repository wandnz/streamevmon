package nz.net.wand.streamevmon

/** Allows for optional implicit evidence of additional type parameters.
  *
  * @see [[nz.net.wand.streamevmon.runners.unified.schema.DetectorType.ValueBuilder DetectorType.ValueBuilder]] for example usage
  * @see [[https://stackoverflow.com/a/20016152]] for source
  */
case class Perhaps[E](value: Option[E]) {
  lazy val isDefined: Boolean = value.isDefined
}

object Perhaps {
  implicit def perhaps[F](implicit ev: F = null): Perhaps[F] = Perhaps(Option(ev))
}
