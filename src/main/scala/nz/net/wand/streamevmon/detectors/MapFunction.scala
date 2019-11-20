package nz.net.wand.streamevmon.detectors

import org.apache.flink.api.common.typeinfo.TypeInformation

/** The function that a [[nz.net.wand.streamevmon.detectors.changepoint.Distribution Distribution]] implementation should supply to an
  * object of type I to convert it to a Double. Please note that
  * implementations of this class '''must be standalone named classes''', not
  * anonymous classes or inner classes. If either of these are used, the object
  * will not serialise correctly and your Flink job will not be able to use
  * checkpoints and savepoints correctly.
  *
  * apply(t: I): Double should contain your map function, while apply() only
  * needs to return a new instance of your implementation.
  *
  * @tparam I The type of object that the implementation of this class converts
  *           to O. Should be the same as the I supplied to Distribution.
  * @tparam O The output type of the map function.
  *
  * @see [[nz.net.wand.streamevmon.detectors.changepoint.Distribution Distribution]]
  */
abstract class MapFunction[I: TypeInformation, O: TypeInformation] extends Serializable {
  def apply(t: I): O

  def apply(): MapFunction[I, O]
}
