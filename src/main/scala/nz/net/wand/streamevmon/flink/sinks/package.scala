package nz.net.wand.streamevmon.flink

/** Sinks are the end of a Flink pipeline. In our case, they usually receive
  * input (events) from detectors, and put them somewhere outside this program,
  * such as InfluxDB or a file.
  */
package object sinks {}
