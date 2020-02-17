package nz.net.wand.streamevmon.measurements

/** Measurements from Haberman's survival dataset.
  *
  * This is a simple three-dimensional dataset. Stream and time are reasonably
  * meaningless within its context, so `stream` can be whatever consistent value
  * you want, and `time` can be whatever is convenient for Flink. `defaultValue`
  * doesn't make a ton of sense either, so it's just set to `positiveNodes` at
  * random.
  *
  * @see [[https://archive.ics.uci.edu/ml/datasets/Haberman%27s+Survival]]
  */
package object haberman {}
