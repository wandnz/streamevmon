package nz.net.wand.streamevmon.runners.tuner.parameters

class Parameters(
  elems: ParameterInstance[Any]*
) {

  /** Returns the parameters as though they were passed as arguments to main. */
  def getAsArgs: Iterable[String] = elems.flatMap(_.asArg)

  override def toString: String = getAsArgs.mkString(" ")
}
