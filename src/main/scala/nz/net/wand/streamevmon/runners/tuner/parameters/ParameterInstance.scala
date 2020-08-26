package nz.net.wand.streamevmon.runners.tuner.parameters

case class ParameterInstance[T](
  parameter: Parameter[T],
  value    : T
) {
  val name: String = parameter.name

  lazy val asArg: Iterable[String] = Seq(s"--$name", value.toString)

  override def toString: String = asArg.mkString(" ")
}
