package nz.net.wand.streamevmon.parameters

/** A particular value for a parameter specified by a ParameterSpec.
  *
  * @param spec  The ParameterSpec for this instance.
  * @param value The particular value of the parameter. Note that for ordered
  *              types, this should be between `spec.min` and `spec.max`. This
  *              is not enforced in this class, so it is possible to create an
  *              invalid ParameterInstance.
  * @tparam T See [[ParameterSpec]] for details.
  */
case class ParameterInstance[T](
  spec: ParameterSpec[T],
  value: T
) extends Serializable {
  val name: String = spec.name

  // No type annotation here because there's no way to guarantee typing... :(
  // If I could, it would be public and : T.
  private lazy val typedValue = value match {
    case v: String if spec.default.getClass == classOf[java.lang.Integer] || spec.default.getClass == classOf[Int] => v.toDouble.toInt
    case v: String if spec.default.getClass == classOf[java.lang.Long] || spec.default.getClass == classOf[Long] => v.toDouble.toLong
    case v: String if spec.default.getClass == classOf[java.lang.Double] || spec.default.getClass == classOf[Double] => v.toDouble
    case v: String if spec.default.getClass == classOf[java.lang.Float] || spec.default.getClass == classOf[Float] => v.toFloat
    case _ => value
  }

  lazy val asArg: Iterable[String] = Seq(s"--$name", typedValue.toString)

  override def toString: String = asArg.mkString(" ")
}

object ParameterInstance {

  /** A ParameterInstance with a corresponding Spec that only allows one value.
    */
  class Constant[T](override val value: T) extends ParameterInstance[T](
    new ParameterSpec.Constant(value),
    value
  )

}
