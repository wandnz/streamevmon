package nz.net.wand.streamevmon

/** Contains classes representing network measurements, such as those gathered
  * by AMP or perfSONAR Esmond.
  *
  * Measurements are grouped into categories based on their source. Each
  * category has its own package, and the package objects have further
  * documentation.
  *
  * Any measurement can inherit from one or more of the items in the [[nz.net.wand.streamevmon.measurements.traits `traits`]]
  * package. These signal additional functionality, and as such many detectors
  * require received messages to implement particular traits. The most notable
  * is [[nz.net.wand.streamevmon.measurements.traits.HasDefault HasDefault]],
  * which allows a measurement to provide a single default Double value. See
  * the package object for more details.
  *
  * All measurements should inherit from
  * [[nz.net.wand.streamevmon.measurements.traits.Measurement Measurement]], or
  * one of its more specific subtypes. See the documentation for Measurement for
  * some notes on what requirements must be implemented.
  */
package object measurements {}
