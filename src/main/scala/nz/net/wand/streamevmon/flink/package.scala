package nz.net.wand.streamevmon

/** Contains classes which can be used as part of a Flink pipeline for any
  * detection algorithm, including custom sources, processors, and sinks.
  *
  * The algorithms themselves should be placed in the detectors package, but
  * many will be implemented as ProcessFunctions.
  *
  * To define the data pipeline for a new algorithm, a new entrypoint should
  * be created in the runners package.
  */
package object flink {}
