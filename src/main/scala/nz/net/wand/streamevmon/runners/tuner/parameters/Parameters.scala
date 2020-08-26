package nz.net.wand.streamevmon.runners.tuner.parameters

class Parameters(
  val elems: ParameterInstance[Any]*
) extends Serializable {

  /** Returns the parameters as though they were passed as arguments to main. */
  def getAsArgs: Iterable[String] = elems.flatMap(_.asArg)

  override def toString: String = getAsArgs.mkString(" ")
}
