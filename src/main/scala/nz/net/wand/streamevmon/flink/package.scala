package nz.net.wand.streamevmon

/** Contains classes which can be used as part of a Flink pipeline for any
  * detection algorithm, including custom sources, sinks, and other stuff.
  *
  * Despite being implemented as ProcessFunctions, detectors should be placed
  * in a corresponding subpackage within [[nz.net.wand.streamevmon.detectors `detectors`]].
  */
package object flink {}
