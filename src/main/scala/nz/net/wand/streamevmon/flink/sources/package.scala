package nz.net.wand.streamevmon.flink

/** Sinks are the beginning of a Flink pipeline. In our case, they produce
  * Measurements to be passed to detectors. Measurements are obtained from
  * outside this program.
  *
  * Sources can take two forms: SourceFunction and FileInputFormat.
  * SourceFunctions allow gathering data with arbitrary methods, while a
  * FileInputFormat is specifically for reading files, usually from disk.
  */
package object sources {}
