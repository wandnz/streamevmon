package nz.net.wand.streamevmon.parameters

import org.apache.commons.math3.random.RandomDataGenerator

case class ParameterSpec[T](
  name: String,
  default: T,
  min    : Option[T],
  max    : Option[T]
) extends Serializable {

  default match {
    case _: Int | _: Long | _: Double =>
      if (min.isEmpty || max.isEmpty) {
        throw new IllegalArgumentException(s"Must specify min and max values for parameter of type ${default.getClass.getSimpleName}")
      }
    case _ =>
  }

  def getDefault: ParameterInstance[T] = ParameterInstance(this, default)

  def generateRandomInRange(rand: RandomDataGenerator = new RandomDataGenerator()): ParameterInstance[T] = {
    val result = default match {
      case _: Int => rand.nextInt(min.get.asInstanceOf[Int], max.get.asInstanceOf[Int])
      case _: Long => rand.nextLong(min.get.asInstanceOf[Long], max.get.asInstanceOf[Long])
      case _: Double => rand.nextUniform(min.get.asInstanceOf[Double], max.get.asInstanceOf[Double])
      case _ => throw new UnsupportedOperationException(s"Can't generate random parameter of type ${default.getClass.getSimpleName}")
    }
    ParameterInstance(this, result.asInstanceOf[T])
  }

  /** Converts this spec into the format expected by SMAC's PCS file. If the
    * fixedValue parameter is not None, the output is a categorical variable
    * with a single class of that value. Otherwise, this spec is replicated,
    * allowing for a range of variable values.
    */
  def toSmacString(fixedValue: Option[T]): String = {
    fixedValue match {
      case Some(value) => s"$name categorical {$value} [$value]"
      case None =>
        val specType = default match {
          case _: Int | _: Long => "integer"
          case _: Float | _: Double => "real"
          case _ => throw new UnsupportedOperationException(s"Can't create SMAC spec for spec with type ${default.getClass.getCanonicalName}")
        }
        (min, max) match {
          case (Some(_), Some(_)) =>
          case _ => throw new UnsupportedOperationException(s"Must specify min and max for SMAC spec")
        }
        s"$name $specType [${min.get},${max.get}] [$default]"
    }
  }
}

object ParameterSpec {

  class Constant[T](value: T) extends ParameterSpec[T](
    s"constant.$value",
    value,
    Some(value),
    Some(value)
  )

}
