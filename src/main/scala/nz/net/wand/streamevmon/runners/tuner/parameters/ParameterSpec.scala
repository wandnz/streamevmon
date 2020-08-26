package nz.net.wand.streamevmon.runners.tuner.parameters

import org.apache.commons.math3.random.RandomDataGenerator

case class ParameterSpec[T](
  name: String,
  default: T,
  min    : Option[T],
  max    : Option[T]
) {

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
}
