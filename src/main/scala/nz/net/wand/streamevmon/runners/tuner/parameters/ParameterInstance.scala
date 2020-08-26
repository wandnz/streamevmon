package nz.net.wand.streamevmon.runners.tuner.parameters

case class ParameterInstance[T](
  spec: ParameterSpec[T],
  value: T
) {
  val name: String = spec.name

  lazy val asArg: Iterable[String] = Seq(s"--$name", value.toString)

  override def toString: String = asArg.mkString(" ")
}
