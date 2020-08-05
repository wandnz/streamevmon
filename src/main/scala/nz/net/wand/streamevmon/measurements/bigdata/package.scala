package nz.net.wand.streamevmon.measurements

/** This package contains representations of the output of the
  * [[https://github.com/jacobvw/libtrace-bigdata Libtrace-Bigdata]] packet
  * analysis tool, created by Jacob van Walraven. The data can be gathered from
  * InfluxDB either as soon as it is inserted (via an InfluxSourceFunction) or
  * historical data can be queried.
  *
  * Currently, only the flow_statistics table is supported, and is represented
  * by the [[nz.net.wand.streamevmon.measurements.bigdata.Flow Flow]] class.
  * While it has many fields, most are optional. The source and destination
  * location fields are in order to support the output of the Maxmind plugin,
  * which gives geolocation for IP addresses. These can also be accessed via the
  * [[nz.net.wand.streamevmon.measurements.bigdata.Flow.Endpoint Endpoint]]
  * objects named `source` and `destination`.
  */
package object bigdata {}
